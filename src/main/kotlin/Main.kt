import com.google.gson.Gson
import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.sql.*
import java.sql.Connection
import kotlin.concurrent.timer


lateinit var bot: TelegramBot

var lastUserState = UserState.STATE_INIT

enum class UserState {
    STATE_NONE,
    STATE_INIT,
    STATE_GET_MOBILE,
    STATE_GET_CODE,
    STATE_ACTIVE_USER,
}

var userList: ArrayList<User> = arrayListOf()

fun main(args: Array<String>) {
    val token = "2033023004:6ldnXC5ILP451w6fpDNqmoWFE9VvnO1fWHfpzLoT"
    val username = "meedc_bot"
    val botId = "2033023004"

    bot = TelegramBot.Builder(token)
        .apiUrl("https://tapi.bale.ai/")
        .build()


    bot.setUpdatesListener { updates: List<Update?>? ->
         if (updates != null) {
             for (update in updates)
                 handleMessage(update?.message()!!)
         }
         //update_id
         UpdatesListener.CONFIRMED_UPDATES_ALL
     }

    //getUserFromDb("09363667756")


}


private fun sendSms(number: String, txt: String) {
    val esbModel = EsbSmsModel(
        "bale", "bale@123", "send_sms_esb", "", "1000136", "person_id", "حسین", "عابدی", number, txt
    )
    val body: RequestBody = RequestBody.create("application/json".toMediaType(), Gson().toJson(esbModel))

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://eiws.meedc.ir/t/sms.meedc/restsms")
        .post(body)
        .header("Content-Type", "application/json")
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        /*for ((name, value) in response.headers) {
            logPlz("$name: $value")
        }*/

        logPlz(response.body!!.string())
    }
}


data class EsbSmsModel(
    val username: String,
    val password: String,
    val func: String,
    val send_date: String,
    val ID: String,
    val TypeID: String,
    val FirstName: String,
    val LastName: String,
    val Number: String,
    val Content: String,
)

fun getUserFromDb(mobile: String): User? {
    Class.forName("org.postgresql.Driver");
    val c: Connection =
        DriverManager.getConnection("jdbc:postgresql://172.18.90.30:5432/raya_portal", "sde", "raya@123")
    val stmt: Statement = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    val rs = stmt.executeQuery("select * from core.\"bale_bot_auth\" WHERE mobile =='$mobile'  ;")
    rs.last()
    val count = rs.row
    rs.beforeFirst()

    var user: User? = null
    if (count > 0) {
        rs.first()

        val id: Int = rs.getInt("id")
        val bale_username: String = rs.getString("bale_username")
        val user_fname: String = rs.getString("user_fname")
        val user_lname: String = rs.getString("user_lname")
        val user_id: String = rs.getString("user_id")
        val mobile: String = rs.getString("mobile")
        val ldap_username: String = rs.getString("ldap_username")
        val ldap_peo_id: String = rs.getString("ldap_peo_id")
        val verify_code: String = rs.getString("verify_code")
        val verify_code_expire_time: Timestamp = rs.getTimestamp("verify_code_expire_time")

        user = User(id, bale_username, user_fname, user_lname, user_id, mobile, ldap_username, ldap_peo_id, verify_code, verify_code_expire_time)


        /*   while (rs.next()) {
               val id: Int = rs.getInt("id")
               val answer: String = rs.getString("answer")
               logPlz("id" + answer)
           }*/
    }

    stmt.close()
    c.close()

    return user;
}

fun updateUser(user: User) {
    Class.forName("org.postgresql.Driver");
    val c: Connection =
        DriverManager.getConnection("jdbc:postgresql://172.18.90.30:5432/raya_portal", "sde", "raya@123")
    val stmt: Statement = c.createStatement();
    stmt.execute(
        "INSERT INTO core.bale_bot_auth(" +
                "  bale_username, user_fname, user_lname, user_id, mobile, ldap_username," +
                "    ldap_peo_id, verify_code, verify_code_expire_time) VALUES " +
                "('${user.username}', '${user.userFName}', '${user.userLName}', '${user.userId}','${user.userMobile}'," +
                "'', '', '${user.verifyCode}', ${user.verifyCodeExpireTime});"
    )
    stmt.close()
    c.close()
}

fun saveMessageOnDb(userId: String, chatId: String, msg: String, answer: String) {
    Class.forName("org.postgresql.Driver");
    val c: Connection =
        DriverManager.getConnection("jdbc:postgresql://172.18.90.30:5432/raya_portal", "sde", "raya@123")
    val stmt: Statement = c.createStatement();
    /*val rs = stmt.executeQuery("select * from core.\"bale_bot_messages\" ;")
    while (rs.next()) {
        val id: Int = rs.getInt("id")
        val answer: String = rs.getString("answer")
        logPlz("id" + answer)
    }*/
    stmt.execute(
        "INSERT INTO core.bale_bot_messages(" +
                " user_id, chat_id, msg, answer, time_msg, time_answer)" +
                "VALUES ('$userId', '$chatId', '$msg', '$answer', current_timestamp,  current_timestamp);"
    )
    stmt.close()
    c.close()


}

fun handleMessage(msg: Message) {
    //message_id
    val user = msg.from()!!
    //first_name //username //
    val chat = msg.chat()!!
    //id  first_name='Javad Ghane', last_name='' username  photo
    val txt = msg.text()
    val contact = msg.contact()


    //todo check user in db
    //fill user object here
    //if chat id is login show welcome txt 
    //else 
    //todo show welcome txt and make auth 

    if (txt != null) {

        logPlz("new Msg from:" + user.id() + " username" + chat.username() + " txt:" + txt)
        if (txt.equals("ثبت دستی شماره")) {
            val answer = "لطفا شماره همراه خود را به صورت 11 رقمی مشابه 09123456789 وارد نمایید."
            sendMsg(chat.id().toString(), answer)
            saveMessageOnDb(user.id().toString(), chat.id().toString(), txt, answer)
        } else if (txt.length == 11 && txt.startsWith("09")) {
            val randomPin = (Math.random() * 90000).toInt() + 10000
            val answer = "کد فعالسازی شما " + randomPin + " می‌باشد. شرکت توزیع برق مشهد"
            sendSms(contact.phoneNumber(),answer )
            saveMessageOnDb(user.id().toString(), chat.id().toString(), txt, answer)
        } else if (txt.length == 5 && txt.toIntOrNull() != null) {
            val answer = "حساب کاربری شما فعال شد. هر زمان مایل به غیرفعالسازی ارسال پیام توسط ربات بودید دستور stop یا لغو را ارسال نمایید."
            sendMsg(chat.id().toString(), answer)
            saveMessageOnDb(user.id().toString(), chat.id().toString(), txt, answer)

            sendMsg(chat.id().toString(), "جلسه معاونت هوشمند سازی و 121 - سه شنبه ساعت 12.30       \n برای ارسال نشدن پیام لغو یا stop را ارسال کنید")
        } else {
            val answer =
                " سلام " + chat.firstName() + chat.lastName() + "، به ربات توزیع برق مشهد خوش آمدید. لطفا جهت احراز هویت شماره همراه خود را به ربات ارسال نمایید."
            sendMsgKeyboard(chat.id().toString(), answer)
            saveMessageOnDb(user.id().toString(), chat.id().toString(), txt, answer)
        }

    }

    if (contact != null) {
        //closeKeyboard(chat.id().toString())
        logPlz("new Msg from:" + user.id() + " username" + chat.username() + " contact:" + contact.phoneNumber())
        val randomPin = (Math.random() * 90000).toInt() + 10000
        sendSms(contact.phoneNumber(), "کد فعالسازی شما " + randomPin + " می‌باشد. شرکت توزیع برق مشهد")
        val codeMsg =
            " یک کد فعالسازی به شماره همراه شما " + "(" + contact.phoneNumber() + ")" + " ارسال شد. لطفا کد را پس از دریافت به ربات ارسال کنید."
        sendMsg(chat.id().toString(), codeMsg, true)
        saveMessageOnDb(user.id().toString(), chat.id().toString(), contact.phoneNumber(), codeMsg)
    }
}

fun closeKeyboard(chatId: String) {
    //val replyKeyboardRemove: Keyboard = ReplyKeyboardRemove(true)
    // bot.execute(SendMessage(chatId, "Keyboard").replyMarkup(replyKeyboardRemove))
}

fun logPlz(msg: Any) {
    println(msg.toString())
}

fun sendMsg(chatId: String, msg: String, isShowCount: Boolean = false) {
    //val response: SendResponse = bot.execute(SendMessage(chatId, msg))
    val request = SendMessage(chatId, msg)
    bot.execute(request, object : Callback<SendMessage, SendResponse> {
        override fun onResponse(request: SendMessage?, response: SendResponse?) {
            logPlz("OnDone")

            if (isShowCount) {
                val msgId = response?.message()?.messageId()
                sendCounter(chatId, msgId!!, msg)
            }
        }

        override fun onFailure(request: SendMessage?, e: IOException?) {
            logPlz("onFailure" + e?.message)
        }
    })
}

fun sendCounter(chatId: String, msgId: Int, msg: String) {
    var allTime = 20
    timer(initialDelay = 0L, period = 1000L) {
        allTime -= 1
        val editMessageText = EditMessageText(chatId, msgId, msg + " (" + allTime + " ثانیه)")
            .parseMode(ParseMode.HTML)
        bot.execute(editMessageText)
        if (allTime == 0) {
            sendMsgKeyboard(
                chatId,
                "زمان به پایان رسید. لطفا مجددا شماره همراه خود را ارسال نمایید تا کد فعالسازی برای شما ارسال شود",
                msgId
            )
            this.cancel()
        }
        //.disableWebPagePreview(true)
    }
}

fun sendMsgKeyboard(chatId: String, msg: String, msgId: Int = 0) {
    val keyboard: Keyboard = ReplyKeyboardMarkup(
        KeyboardButton("ثبت دستی شماره"),
        KeyboardButton("ارسال شماره همراه").requestContact(true),
        //KeyboardButton("location").requestLocation(true)
    )

    val request = SendMessage(chatId, msg).replyMarkup(keyboard).replyToMessageId(msgId)
    bot.execute(request, object : Callback<SendMessage, SendResponse> {
        override fun onResponse(request: SendMessage?, response: SendResponse?) {
            logPlz("OnDone")
        }

        override fun onFailure(request: SendMessage?, e: IOException?) {
            logPlz("onFailure" + e?.message)
        }
    })
}