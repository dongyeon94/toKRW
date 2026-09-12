import Foundation

/// 앱에서 고를 수 있는 통화 하나.
///
/// `displayUnit` 은 환율을 보여줄 때 쓰는 기준 단위다. 엔이나 동처럼 1단위 값이 아주 작은
/// 통화는 국내 은행 고시처럼 100단위로 보여주는 편이 읽기 쉽다.
struct Currency: Identifiable, Hashable {
    let code: String
    let name: String
    let flag: String
    var displayUnit: Int = 1

    var id: String { code }
}

enum Currencies {

    static let krw = Currency(code: "KRW", name: "대한민국 원", flag: "🇰🇷")

    /// 선택 목록. KRW 는 결과 통화라서 목록에 넣지 않는다.
    static let all: [Currency] = [
        Currency(code: "USD", name: "미국 달러", flag: "🇺🇸"),
        Currency(code: "JPY", name: "일본 엔", flag: "🇯🇵", displayUnit: 100),
        Currency(code: "EUR", name: "유로", flag: "🇪🇺"),
        Currency(code: "CNY", name: "중국 위안", flag: "🇨🇳"),
        Currency(code: "GBP", name: "영국 파운드", flag: "🇬🇧"),
        Currency(code: "HKD", name: "홍콩 달러", flag: "🇭🇰"),
        Currency(code: "TWD", name: "대만 달러", flag: "🇹🇼"),
        Currency(code: "SGD", name: "싱가포르 달러", flag: "🇸🇬"),
        Currency(code: "THB", name: "태국 바트", flag: "🇹🇭"),
        Currency(code: "VND", name: "베트남 동", flag: "🇻🇳", displayUnit: 100),
        Currency(code: "PHP", name: "필리핀 페소", flag: "🇵🇭"),
        Currency(code: "IDR", name: "인도네시아 루피아", flag: "🇮🇩", displayUnit: 100),
        Currency(code: "MYR", name: "말레이시아 링깃", flag: "🇲🇾"),
        Currency(code: "AUD", name: "호주 달러", flag: "🇦🇺"),
        Currency(code: "NZD", name: "뉴질랜드 달러", flag: "🇳🇿"),
        Currency(code: "CAD", name: "캐나다 달러", flag: "🇨🇦"),
        Currency(code: "CHF", name: "스위스 프랑", flag: "🇨🇭"),
        Currency(code: "INR", name: "인도 루피", flag: "🇮🇳"),
        Currency(code: "AED", name: "아랍에미리트 디르함", flag: "🇦🇪"),
        Currency(code: "SAR", name: "사우디 리얄", flag: "🇸🇦"),
        Currency(code: "TRY", name: "튀르키예 리라", flag: "🇹🇷"),
        Currency(code: "RUB", name: "러시아 루블", flag: "🇷🇺"),
        Currency(code: "BRL", name: "브라질 헤알", flag: "🇧🇷"),
        Currency(code: "MXN", name: "멕시코 페소", flag: "🇲🇽"),
        Currency(code: "SEK", name: "스웨덴 크로나", flag: "🇸🇪"),
        Currency(code: "NOK", name: "노르웨이 크로네", flag: "🇳🇴"),
        Currency(code: "DKK", name: "덴마크 크로네", flag: "🇩🇰"),
        Currency(code: "PLN", name: "폴란드 즈워티", flag: "🇵🇱"),
        Currency(code: "CZK", name: "체코 코루나", flag: "🇨🇿"),
        Currency(code: "HUF", name: "헝가리 포린트", flag: "🇭🇺", displayUnit: 100),
        Currency(code: "ZAR", name: "남아공 랜드", flag: "🇿🇦"),
        Currency(code: "EGP", name: "이집트 파운드", flag: "🇪🇬"),
        Currency(code: "ILS", name: "이스라엘 셰켈", flag: "🇮🇱"),
        Currency(code: "MOP", name: "마카오 파타카", flag: "🇲🇴"),
        Currency(code: "MNT", name: "몽골 투그릭", flag: "🇲🇳", displayUnit: 100),
        Currency(code: "KZT", name: "카자흐스탄 텡게", flag: "🇰🇿", displayUnit: 100),
        Currency(code: "UZS", name: "우즈베키스탄 숨", flag: "🇺🇿", displayUnit: 100),
        Currency(code: "NPR", name: "네팔 루피", flag: "🇳🇵"),
        Currency(code: "LKR", name: "스리랑카 루피", flag: "🇱🇰"),
        Currency(code: "PKR", name: "파키스탄 루피", flag: "🇵🇰"),
        Currency(code: "BDT", name: "방글라데시 타카", flag: "🇧🇩"),
        Currency(code: "KHR", name: "캄보디아 리엘", flag: "🇰🇭", displayUnit: 100),
        Currency(code: "LAK", name: "라오스 킵", flag: "🇱🇦", displayUnit: 100),
        Currency(code: "MMK", name: "미얀마 짯", flag: "🇲🇲", displayUnit: 100),
        Currency(code: "CLP", name: "칠레 페소", flag: "🇨🇱", displayUnit: 100),
        Currency(code: "COP", name: "콜롬비아 페소", flag: "🇨🇴", displayUnit: 100),
        Currency(code: "PEN", name: "페루 솔", flag: "🇵🇪"),
        Currency(code: "ARS", name: "아르헨티나 페소", flag: "🇦🇷", displayUnit: 100),
        Currency(code: "UAH", name: "우크라이나 흐리브냐", flag: "🇺🇦"),
        Currency(code: "RON", name: "루마니아 레우", flag: "🇷🇴"),
        Currency(code: "QAR", name: "카타르 리얄", flag: "🇶🇦"),
        Currency(code: "KWD", name: "쿠웨이트 디나르", flag: "🇰🇼"),
    ]

    private static let byCode: [String: Currency] =
        Dictionary(uniqueKeysWithValues: all.map { ($0.code, $0) })

    static func of(_ code: String) -> Currency {
        byCode[code] ?? Currency(code: code, name: code, flag: "🏳️")
    }

    static let `default` = of("USD")
}
