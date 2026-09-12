import Foundation

/// USD 를 기준으로 한 환율 표 한 벌.
///
/// 통화 하나마다 따로 조회하지 않고 USD 기준 표를 한 번만 받아서 교차 환율을 계산한다.
/// 그래서 통화를 바꿔도 네트워크 호출이 다시 일어나지 않는다.
struct RateSnapshot: Equatable {
    let rates: [String: Double]
    let fetchedAt: Date
    let providerLabel: String

    /// 1 `code` 가 몇 원인지. 표에 없는 통화면 nil.
    func krwPerUnit(_ code: String) -> Double? {
        if code == "KRW" { return 1 }
        guard let krw = rates["KRW"], let unit = rates[code], unit > 0 else { return nil }
        return krw / unit
    }

    func supports(_ code: String) -> Bool {
        code == "KRW" || rates[code] != nil
    }
}

enum RateError: LocalizedError {
    case allProvidersFailed([String])

    var errorDescription: String? {
        switch self {
        case .allProvidersFailed(let reasons):
            return "환율을 가져오지 못했습니다 (\(reasons.joined(separator: " / ")))"
        }
    }
}

actor RateRepository {

    private struct Provider {
        let label: String
        let url: URL
    }

    /// 둘 다 키가 필요 없는 무료 공개 API 다. 위에서부터 차례로 시도한다.
    private let providers = [
        Provider(label: "open.er-api.com", url: URL(string: "https://open.er-api.com/v6/latest/USD")!),
        Provider(label: "frankfurter.dev", url: URL(string: "https://api.frankfurter.dev/v1/latest?base=USD")!),
    ]

    private let defaults = UserDefaults.standard
    private let ratesKey = "tokrw.rates"
    private let fetchedAtKey = "tokrw.fetchedAt"
    private let providerKey = "tokrw.provider"

    /// 저장해 둔 마지막 결과. 앱을 켜자마자 보여줄 값이고, 오프라인일 때의 대비책이기도 하다.
    nonisolated func cached() -> RateSnapshot? {
        let defaults = UserDefaults.standard
        guard let stored = defaults.dictionary(forKey: "tokrw.rates") as? [String: Double],
              !stored.isEmpty else { return nil }
        let at = defaults.double(forKey: "tokrw.fetchedAt")
        let provider = defaults.string(forKey: "tokrw.provider") ?? "저장된 값"
        return RateSnapshot(
            rates: stored,
            fetchedAt: Date(timeIntervalSince1970: at),
            providerLabel: provider
        )
    }

    /// 새 환율을 받아 온다. 1차 공급처가 실패하면 2차로 넘어가고, 둘 다 실패하면 오류를 던진다.
    func refresh() async throws -> RateSnapshot {
        var reasons: [String] = []

        for provider in providers {
            do {
                var request = URLRequest(url: provider.url)
                request.timeoutInterval = 10
                request.setValue("application/json", forHTTPHeaderField: "Accept")
                request.cachePolicy = .reloadIgnoringLocalCacheData

                let (data, response) = try await URLSession.shared.data(for: request)
                if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                    throw RateError.allProvidersFailed(["HTTP \(http.statusCode)"])
                }

                let rates = try Self.parse(data)
                guard rates["KRW"] != nil else {
                    throw RateError.allProvidersFailed(["응답에 KRW 가 없습니다"])
                }

                let snapshot = RateSnapshot(
                    rates: rates,
                    fetchedAt: Date(),
                    providerLabel: provider.label
                )
                defaults.set(rates, forKey: ratesKey)
                defaults.set(snapshot.fetchedAt.timeIntervalSince1970, forKey: fetchedAtKey)
                defaults.set(provider.label, forKey: providerKey)
                return snapshot
            } catch {
                let reason = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
                reasons.append("\(provider.label): \(reason)")
            }
        }

        throw RateError.allProvidersFailed(reasons)
    }

    /// 두 공급처 모두 `rates` 아래에 코드 → 숫자 형태로 내려 준다.
    private static func parse(_ data: Data) throws -> [String: Double] {
        let root = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let table = root?["rates"] as? [String: Any] else {
            throw RateError.allProvidersFailed(["응답 형식을 읽을 수 없습니다"])
        }

        var out: [String: Double] = [:]
        out.reserveCapacity(table.count)
        for (code, value) in table {
            if let number = value as? NSNumber {
                let double = number.doubleValue
                if double.isFinite && double > 0 { out[code] = double }
            }
        }
        out["USD"] = 1
        return out
    }
}
