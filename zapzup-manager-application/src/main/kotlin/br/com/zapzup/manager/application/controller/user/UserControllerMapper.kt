package br.com.zapzup.manager.application.controller.user

import br.com.zapzup.manager.api.user.request.CreateUserRequest
import br.com.zapzup.manager.api.user.response.CreateUserResponse
import br.com.zapzup.manager.api.user.response.UserResponse
import br.com.zapzup.manager.domain.to.user.CreateUserTO
import br.com.zapzup.manager.domain.to.user.UserTO

fun CreateUserRequest.toDomain() = CreateUserTO(
    name = this.name,
    username = this.username,
    email = this.email,
    password = this.password
)

fun UserTO.toCreateUserResponse() =
    CreateUserResponse(
        id = this.id,
        name = this.name,
        username = this.username,
        email = this.email,
        createdAt = this.createdAt
    )

fun UserTO.toResponse() =
    UserResponse(
        id = this.id,
        name = this.name,
        username = this.username,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt
    )

fun toResponseList(list: List<UserTO>) =
    list.map { it.toResponse() }