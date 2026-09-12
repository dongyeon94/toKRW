package com.example.tokrw

/**
 * 앱에서 고를 수 있는 통화 하나를 나타낸다.
 *
 * [displayUnit] 은 환율을 보여줄 때 쓰는 기준 단위다. 엔이나 동처럼 1단위 값이 아주 작은
 * 통화는 국내 은행 고시처럼 100단위로 보여주는 편이 읽기 쉽다.
 */
data class Currency(
    val code: String,
    val name: String,
    val flag: String,
    val displayUnit: Int = 1,
) {
    val label: String get() = "$flag  $code"
}

object Currencies {

    val KRW = Currency("KRW", "대한민국 원", "🇰🇷")

    /** 선택 목록. KRW 는 결과 통화라서 목록에 넣지 않는다. */
    val all: List<Currency> = listOf(
        Currency("USD", "미국 달러", "🇺🇸"),
        Currency("JPY", "일본 엔", "🇯🇵", displayUnit = 100),
        Currency("EUR", "유로", "🇪🇺"),
        Currency("CNY", "중국 위안", "🇨🇳"),
        Currency("GBP", "영국 파운드", "🇬🇧"),
        Currency("HKD", "홍콩 달러", "🇭🇰"),
        Currency("TWD", "대만 달러", "🇹🇼"),
        Currency("SGD", "싱가포르 달러", "🇸🇬"),
        Currency("THB", "태국 바트", "🇹🇭"),
        Currency("VND", "베트남 동", "🇻🇳", displayUnit = 100),
        Currency("PHP", "필리핀 페소", "🇵🇭"),
        Currency("IDR", "인도네시아 루피아", "🇮🇩", displayUnit = 100),
        Currency("MYR", "말레이시아 링깃", "🇲🇾"),
        Currency("AUD", "호주 달러", "🇦🇺"),
        Currency("NZD", "뉴질랜드 달러", "🇳🇿"),
        Currency("CAD", "캐나다 달러", "🇨🇦"),
        Currency("CHF", "스위스 프랑", "🇨🇭"),
        Currency("INR", "인도 루피", "🇮🇳"),
        Currency("AED", "아랍에미리트 디르함", "🇦🇪"),
        Currency("SAR", "사우디 리얄", "🇸🇦"),
        Currency("TRY", "튀르키예 리라", "🇹🇷"),
        Currency("RUB", "러시아 루블", "🇷🇺"),
        Currency("BRL", "브라질 헤알", "🇧🇷"),
        Currency("MXN", "멕시코 페소", "🇲🇽"),
        Currency("SEK", "스웨덴 크로나", "🇸🇪"),
        Currency("NOK", "노르웨이 크로네", "🇳🇴"),
        Currency("DKK", "덴마크 크로네", "🇩🇰"),
        Currency("PLN", "폴란드 즈워티", "🇵🇱"),
        Currency("CZK", "체코 코루나", "🇨🇿"),
        Currency("HUF", "헝가리 포린트", "🇭🇺", displayUnit = 100),
        Currency("ZAR", "남아공 랜드", "🇿🇦"),
        Currency("EGP", "이집트 파운드", "🇪🇬"),
        Currency("ILS", "이스라엘 셰켈", "🇮🇱"),
        Currency("MOP", "마카오 파타카", "🇲🇴"),
        Currency("MNT", "몽골 투그릭", "🇲🇳", displayUnit = 100),
        Currency("KZT", "카자흐스탄 텡게", "🇰🇿", displayUnit = 100),
        Currency("UZS", "우즈베키스탄 숨", "🇺🇿", displayUnit = 100),
        Currency("NPR", "네팔 루피", "🇳🇵"),
        Currency("LKR", "스리랑카 루피", "🇱🇰"),
        Currency("PKR", "파키스탄 루피", "🇵🇰"),
        Currency("BDT", "방글라데시 타카", "🇧🇩"),
        Currency("KHR", "캄보디아 리엘", "🇰🇭", displayUnit = 100),
        Currency("LAK", "라오스 킵", "🇱🇦", displayUnit = 100),
        Currency("MMK", "미얀마 짯", "🇲🇲", displayUnit = 100),
        Currency("CLP", "칠레 페소", "🇨🇱", displayUnit = 100),
        Currency("COP", "콜롬비아 페소", "🇨🇴", displayUnit = 100),
        Currency("PEN", "페루 솔", "🇵🇪"),
        Currency("ARS", "아르헨티나 페소", "🇦🇷", displayUnit = 100),
        Currency("UAH", "우크라이나 흐리브냐", "🇺🇦"),
        Currency("RON", "루마니아 레우", "🇷🇴"),
        Currency("QAR", "카타르 리얄", "🇶🇦"),
        Currency("KWD", "쿠웨이트 디나르", "🇰🇼"),
    )

    private val byCode = all.associateBy { it.code }

    fun of(code: String): Currency = byCode[code] ?: Currency(code, code, "🏳️")

    val default: Currency = of("USD")
}
