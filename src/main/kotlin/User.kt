import java.sql.Time
import java.sql.Timestamp

data class User(
    val id: Int = 0,
    val username: String,
    val userFName: String,
    val userLName: String,
    val userId: String,
    val userMobile: String,
    val userLdapUsername: String,
    val userLdapPeoId: String,
    val verifyCode: String,
    val verifyCodeExpireTime: Timestamp,
    val userState: UserState = UserState.STATE_INIT,
)