package dev.sandrocaseiro.template.services

import dev.sandrocaseiro.template.clients.UserClient
import dev.sandrocaseiro.template.exceptions.AppErrors
import dev.sandrocaseiro.template.exceptions.AppException
import dev.sandrocaseiro.template.models.AResponse
import dev.sandrocaseiro.template.models.api.AUserByIdResp
import dev.sandrocaseiro.template.models.api.AUserUpdateReq
import dev.sandrocaseiro.template.models.domain.EUser
import dev.sandrocaseiro.template.models.jpa.JUserGroup
import dev.sandrocaseiro.template.models.service.SUser
import dev.sandrocaseiro.template.repositories.UserRepository
import dev.sandrocaseiro.template.security.IAuthenticationInfo
import dev.sandrocaseiro.template.utils.deserialize
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class UserService(
    private val authInfo: IAuthenticationInfo,
    private val userRepository: UserRepository,
    private val userApiClient: UserClient
) {
    fun create(user: EUser): EUser {
        if (userRepository.findByUsername(user.email) != null)
            AppErrors.USERNAME_ALREADY_EXISTS.throws()

        user.active = true
        return userRepository.save(user)
    }

    @Transactional
    fun update(user: EUser) {
        val dbUser: EUser = userRepository.findById(user.id).orElseThrow { AppErrors.ITEM_NOT_FOUND_ERROR.throws() }

        dbUser.name = user.name
        dbUser.cpf = user.cpf
        dbUser.password = user.password
        dbUser.groupId = user.groupId
        dbUser.roles = user.roles

        userRepository.save(dbUser)
    }

    @Transactional
    fun updateBalance(id: Int, balance: BigDecimal) {
        val updatedUsers: Int = userRepository.updateBalance(id, balance)
        if (updatedUsers == 0)
            AppErrors.ITEM_NOT_FOUND_ERROR.throws()
    }

    fun updateBalanceApi(id: Int, balance: BigDecimal) {
        val resp = userApiClient.updateBalance(id, AUserUpdateReq(balance))
        if (resp.status() == HttpStatus.NOT_FOUND.value()) AppErrors.NOT_FOUND_ERROR.throws()
        else if (HttpStatus.valueOf(resp.status()).isError) AppErrors.API_ERROR.throws()
    }

    @Transactional
    fun delete(id: Int) {
        if (authInfo.getId() == id)
            throw AppException.of(AppErrors.FORBIDDEN_ERROR)

        val user: EUser = userRepository.findById(id).orElseThrow { AppErrors.NOT_FOUND_ERROR.throws() }
        user.active = false

        userRepository.save(user)
    }

    fun findById(id: Int): JUserGroup = userRepository.findOneById(id) ?: AppErrors.ITEM_NOT_FOUND_ERROR.throws()

    fun findByIdApi(id: Int): SUser {
        val resp = userApiClient.getById(id)
        if (resp.status() == HttpStatus.NOT_FOUND.value()) AppErrors.NOT_FOUND_ERROR.throws()
        else if (HttpStatus.valueOf(resp.status()).isError) AppErrors.API_ERROR.throws()

        val userResp = resp.body().deserialize<AResponse<AUserByIdResp>>()!!

        return SUser(userResp.data!!.id, userResp.data.name, userResp.data.email)
    }

    fun searchByName(name: String): List<EUser> =  userRepository.searchByName(name)

    fun searchByCpf(cpf: String): List<EUser> = userRepository.searchByCpf(cpf)

    fun findByGroup(groupId: Int): List<EUser> = userRepository.findByGroup(groupId)

    fun findAllActive(pageable: Pageable): Page<EUser> = userRepository.findAllActive(pageable)

    fun findAll(): List<EUser> = userRepository.findAll()
}
