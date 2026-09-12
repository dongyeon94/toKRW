import Foundation
import SwiftUI

@MainActor
final class ConverterViewModel: ObservableObject {

    @Published var currency: Currency = Currencies.default
    @Published private(set) var amountInput: String = "0"
    @Published private(set) var snapshot: RateSnapshot?
    @Published private(set) var loading = false
    @Published private(set) var errorMessage: String?

    private let repository = RateRepository()
    private let maxDigits = 12
    private let maxDecimals = 2

    /// 공급처가 하루에 한 번 값을 올리기 때문에 그보다 자주 받을 이유가 없다.
    private let autoRefreshInterval: TimeInterval = 60 * 60 * 24

    init() {
        snapshot = repository.cached()
        // 저장해 둔 값이 없거나 하루 넘게 묵었으면 바로 새로 받아 온다.
        let age = snapshot.map { Date().timeIntervalSince($0.fetchedAt) } ?? .infinity
        if age > autoRefreshInterval {
            Task { await refresh() }
        }
    }

    var amount: Double { Double(amountInput) ?? 0 }

    /// 1 `currency` 당 원화. 아직 환율이 없거나 지원하지 않는 통화면 nil.
    var krwPerUnit: Double? { snapshot?.krwPerUnit(currency.code) }

    var convertedKrw: Double? { krwPerUnit.map { $0 * amount } }

    var unsupported: Bool {
        guard let snapshot else { return false }
        return !snapshot.supports(currency.code)
    }

    func refresh() async {
        guard !loading else { return }
        loading = true
        errorMessage = nil
        do {
            snapshot = try await repository.refresh()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
        loading = false
    }

    func select(_ currency: Currency) {
        self.currency = currency
    }

    func tapDigit(_ digit: Character) {
        if amountInput == "0" {
            amountInput = String(digit)
            return
        }

        let parts = amountInput.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
        if parts.count == 2 && parts[1].count >= maxDecimals { return }
        if amountInput.filter(\.isNumber).count >= maxDigits { return }

        amountInput.append(digit)
    }

    func tapDecimalPoint() {
        guard !amountInput.contains(".") else { return }
        amountInput.append(".")
    }

    func tapBackspace() {
        amountInput.removeLast()
        if amountInput.isEmpty { amountInput = "0" }
    }

    func clear() {
        amountInput = "0"
    }
}

// MARK: - 숫자 표시

private let groupingFormatter: NumberFormatter = {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    return formatter
}()

private func rateFormatter(fractionDigits: Int) -> NumberFormatter {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.minimumFractionDigits = fractionDigits
    formatter.maximumFractionDigits = fractionDigits
    return formatter
}

/// 키패드로 찍는 중인 값을 그대로 보여 준다. "1234." 처럼 입력 중인 소수점도 살린다.
func formatAmountInput(_ input: String) -> String {
    let hasTrailingDot = input.hasSuffix(".")
    let parts = input.split(separator: ".", maxSplits: 1, omittingEmptySubsequences: false)
    let integerPart = String(parts.first ?? "0")
    let decimals = parts.count == 2 ? String(parts[1]) : ""

    let grouped = groupingFormatter.string(from: NSNumber(value: Int(integerPart) ?? 0)) ?? integerPart
    if !decimals.isEmpty { return "\(grouped).\(decimals)" }
    if hasTrailingDot { return "\(grouped)." }
    return grouped
}

/// 원화는 소수점을 버리고 천 단위로 끊어 보여 준다.
func formatKrw(_ value: Double) -> String {
    groupingFormatter.string(from: NSNumber(value: value.rounded())) ?? "\(Int(value.rounded()))"
}

/// 환율처럼 크기를 모르는 값은 자릿수에 맞춰 소수점 개수를 정한다.
func formatRate(_ value: Double) -> String {
    let digits: Int
    switch value {
    case 1000...: digits = 2
    case 1..<1000: digits = 4
    default: digits = 6
    }
    return rateFormatter(fractionDigits: digits).string(from: NSNumber(value: value)) ?? "\(value)"
}

func formatUpdatedAt(_ date: Date) -> String {
    let minutes = Int(Date().timeIntervalSince(date) / 60)
    switch minutes {
    case ..<1: return "방금 전 기준"
    case ..<60: return "\(minutes)분 전 기준"
    case ..<(60 * 24): return "\(minutes / 60)시간 전 기준"
    default: return "\(minutes / (60 * 24))일 전 기준"
    }
}
