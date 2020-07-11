package br.com.zapzup.manager.service.chat.impl

import br.com.zapzup.manager.commons.exceptions.DuplicatedIdException
import br.com.zapzup.manager.domain.entity.Chat
import br.com.zapzup.manager.domain.entity.User
import br.com.zapzup.manager.domain.enums.ChatStatusEnum
import br.com.zapzup.manager.domain.to.chat.ChatTO
import br.com.zapzup.manager.domain.to.chat.CreateChatTO
import br.com.zapzup.manager.domain.to.chat.CreateGroupChatTO
import br.com.zapzup.manager.repository.ChatRepository
import br.com.zapzup.manager.service.chat.IChatService
import br.com.zapzup.manager.service.chat.mapper.toTO
import br.com.zapzup.manager.service.file.IFileService
import br.com.zapzup.manager.service.file.mapper.toEntity
import br.com.zapzup.manager.service.user.IUserService
import br.com.zapzup.manager.service.user.mapper.toEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
open class ChatService(
    private val chatRepository: ChatRepository,
    private val fileService: IFileService,
    private val userService: IUserService
) : IChatService {

    override fun create(createChatTO: CreateChatTO): ChatTO {
        val userTO = userService.getUserById(createChatTO.userId)

        val chat = chatRepository.save(
            Chat(createdBy = userTO.name, users = mutableListOf(userTO.toEntity()))
        )

        return chat.toTO()
    }

    @Transactional
    override fun createGroupChat(createGroupChatTO: CreateGroupChatTO, groupIcon: MultipartFile): ChatTO {
        val fileTO = fileService.saveFile(groupIcon)
        val members: MutableList<User> = mutableListOf()
        val creatorUserTO = userService.getUserById(createGroupChatTO.creatorUserId)

        members.add(creatorUserTO.toEntity())

        createGroupChatTO.members.forEach { member ->
            val userTO = userService.getUserById(member.id)

            if (members.firstOrNull { it.id == userTO.id } !== null) {
                throw DuplicatedIdException(userTO.id)
            }

            members.add(userTO.toEntity())
        }

        val chat = chatRepository.save(
            Chat(
                name = createGroupChatTO.chatName,
                description = createGroupChatTO.chatDescription,
                createdBy = creatorUserTO.username,
                status = ChatStatusEnum.ACTIVE,
                icon = fileTO.toEntity(),
                users = members
            )
        )

        return chat.toTO()
    }
}