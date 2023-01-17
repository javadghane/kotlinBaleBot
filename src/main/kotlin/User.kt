data class User(
    val userId: Int = 0,
    val username: String,
    val userFName: String,
    val userLName: String,
    val userMobile: String,
    val userState: UserState = UserState.STATE_INIT,
)