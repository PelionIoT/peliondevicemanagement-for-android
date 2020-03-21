package com.arm.peliondevicemanagement.components

import com.arm.peliondevicemanagement.constants.APIConstants.KEY_ACCOUNT
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_GRANT_TYPE
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_PASSWORD
import com.arm.peliondevicemanagement.constants.APIConstants.KEY_USERNAME
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.services.CloudAPIService
import com.arm.peliondevicemanagement.services.CloudAPIService.Companion.createJSONRequestBody
import com.arm.peliondevicemanagement.services.data.BaseRepository
import com.arm.peliondevicemanagement.services.data.UserAccountResponse

class CloudRepository(private val cloudAPIService: CloudAPIService): BaseRepository() {

    suspend fun doAuth(username: String, password: String): UserAccountResponse? {
        val userAccountResponse: UserAccountResponse?
        val errorMessage = "Unable to login"

        if(!SharedPrefHelper.getSelectedAccountID().isNullOrBlank()){
            userAccountResponse = doSafeAPIRequest(
                call = { cloudAPIService.doAuth(createJSONRequestBody(
                    KEY_USERNAME to username,
                    KEY_PASSWORD to password,
                    KEY_GRANT_TYPE to KEY_PASSWORD,
                    KEY_ACCOUNT to SharedPrefHelper.getSelectedAccountID().toString()))},
                errorMessage = errorMessage
            )
        } else {
            userAccountResponse = doSafeAPIRequest(
                call = { cloudAPIService.doAuth(createJSONRequestBody(
                    KEY_USERNAME to username,
                    KEY_PASSWORD to password,
                    KEY_GRANT_TYPE to KEY_PASSWORD))},
                errorMessage = errorMessage
            )
        }
        return userAccountResponse
    }

}