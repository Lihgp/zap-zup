package br.com.zapzup.manager.service.chat.impl

import br.com.zapzup.manager.commons.exceptions.ChatNotFoundException
import br.com.zapzup.manager.commons.exceptions.DuplicatedIdException
import br.com.zapzup.manager.domain.entity.Chat
import br.com.zapzup.manager.domain.entity.User
import br.com.zapzup.manager.domain.enums.ChatStatusEnum
import br.com.zapzup.manager.domain.to.chat.ChatTO
import br.com.zapzup.manager.domain.to.chat.CreateGroupChatTO
import br.com.zapzup.manager.domain.to.chat.CreatePrivateChatTO
import br.com.zapzup.manager.repository.ChatRepository
import br.com.zapzup.manager.service.chat.IChatService
import br.com.zapzup.manager.service.chat.mapper.toEntity
import br.com.zapzup.manager.service.chat.mapper.toTO
import br.com.zapzup.manager.service.chat.mapper.toTOList
import br.com.zapzup.manager.service.file.IFileService
import br.com.zapzup.manager.service.file.mapper.toEntity
import br.com.zapzup.manager.service.user.IUserService
import br.com.zapzup.manager.service.user.mapper.toEntity
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@Service
@Transactional
open class ChatService(
    private val chatRepository: ChatRepository,
    private val fileService: IFileService,
    private val userService: IUserService,
    private val messagingTemplate: SimpMessageSendingOperations
) : IChatService {

    override fun createPrivateChat(createPrivateChatTO: CreatePrivateChatTO): ChatTO {
        if (createPrivateChatTO.creatorUserId == createPrivateChatTO.memberId) {
            throw DuplicatedIdException(createPrivateChatTO.memberId)
        }

        val userTO = userService.getUserById(createPrivateChatTO.creatorUserId)
        val memberTO = userService.getUserById(createPrivateChatTO.memberId)

        val chat = chatRepository.save(
            Chat(
                createdBy = userTO.username,
                users = mutableListOf(userTO.toEntity(), memberTO.toEntity()))
        )

        return chat.toTO()
    }

    override fun createGroupChat(createGroupChatTO: CreateGroupChatTO, groupIcon: MultipartFile?): ChatTO {
        val fileTO = fileService.saveFile(groupIcon)
        val members: MutableList<User> = mutableListOf()
        val creatorUserTO = userService.getUserById(createGroupChatTO.creatorUserId)

        members.add(creatorUserTO.toEntity())

        createGroupChatTO.members.forEach { member ->
            if (members.firstOrNull { it.id == member.id } !== null) {
                throw DuplicatedIdException(member.id)
            }

            val userTO = userService.getUserById(member.id)

            members.add(userTO.toEntity())
        }

        val chat = chatRepository.save(
            Chat(
                name = createGroupChatTO.name,
                description = createGroupChatTO.description,
                createdBy = creatorUserTO.username,
                status = ChatStatusEnum.ACTIVE,
                icon = fileTO?.toEntity(),
                users = members
            )
        )

        return chat.toTO()
    }

    override fun findById(id: String): ChatTO =
        chatRepository.findById(id).orElseThrow { ChatNotFoundException() }.toTO()

    override fun updateLastMessageSent(id: String) {
        val chat = findById(id).toEntity()

        chatRepository.save(chat.copy(lastMessageSentAt = OffsetDateTime.now(), status = ChatStatusEnum.ACTIVE))
    }

    override fun sendToUsersChatsOrderedByLastMessageSent(id: String) {
        val chat = findById(id).toEntity()

        chat.users.forEach { user ->
            val chats = chatRepository.findAllByUserId(user.id)

            messagingTemplate.convertAndSend("/topic/chats/${user.id}", chats.toTOList())
        }
    }
}