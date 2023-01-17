import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.*
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import java.io.IOException
import kotlin.concurrent.timer


//https://github.com/JetBrains/Exposed/wiki/Getting-Started
//https://github.com/icerockdev/db-utils/blob/master/sample/src/main/kotlin/com/icerockdev/sample/Main.kt
//https://github.com/hfhbd/postgres-native-sqldelight
//https://github.com/pengrad/java-telegram-bot-api
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


}

fun saveMessageOnDb() {

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
            sendMsg(chat.id().toString(), "لطفا شماره همراه خود را به صورت 11 رقمی مشابه 09123456789 وارد نمایید.")
        } else {
            sendMsgKeyboard(chat.id().toString(), " سلام " + chat.firstName() + chat.lastName() + "، به ربات توزیع برق مشهد خوش آمدید. لطفا جهت احراز هویت شماره همراه خود را به ربات ارسال نمایید.")
        }

    }

    if (contact != null) {
        closeKeyboard(chat.id().toString())
        logPlz("new Msg from:" + user.id() + " username" + chat.username() + " contact:" + contact.phoneNumber())
        val codeMsg = " یک کد فعالسازی به شماره همراه شما " + "(" + contact.phoneNumber() + ")" + " ارسال شد. لطفا کد را پس از دریافت به ربات ارسال کنید."
        sendMsg(chat.id().toString(), codeMsg, true)
    }
}

fun closeKeyboard(chatId: String) {
    val replyKeyboardRemove: Keyboard = ReplyKeyboardRemove(true)
    bot.execute(SendMessage(chatId, "Keyboard").replyMarkup(replyKeyboardRemove))
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
            sendMsgKeyboard(chatId, "زمان به پایان رسید. لطفا مجددا شماره همراه خود را ارسال نمایید تا کد فعالسازی برای شما ارسال شود", msgId)
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