import SwiftUI
import UIKit

private enum Palette {
    // 계산기처럼 쓰는 화면이라 밝기와 무관하게 어두운 배경으로 고정한다.
    static let background = Color(red: 0.043, green: 0.063, blue: 0.125)
    static let surface = Color(red: 0.078, green: 0.102, blue: 0.180)
    static let accent = Color(red: 0.310, green: 0.612, blue: 0.976)
    static let onBackground = Color(red: 0.949, green: 0.961, blue: 1.0)
}

struct ContentView: View {
    @StateObject private var viewModel = ConverterViewModel()
    @State private var pickerOpen = false

    var body: some View {
        ZStack {
            Palette.background.ignoresSafeArea()

            VStack(spacing: 0) {
                header
                Spacer(minLength: 12)
                amountBlock
                Spacer(minLength: 12)
                rateLine
                    .padding(.bottom, 12)
                Keypad(
                    onDigit: viewModel.tapDigit,
                    onDecimalPoint: viewModel.tapDecimalPoint,
                    onBackspace: viewModel.tapBackspace,
                    onClear: viewModel.clear
                )
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .sheet(isPresented: $pickerOpen) {
            CurrencyPicker(
                selected: viewModel.currency,
                snapshot: viewModel.snapshot
            ) { picked in
                viewModel.select(picked)
                pickerOpen = false
            }
        }
    }

    private var header: some View {
        HStack {
            Button {
                pickerOpen = true
            } label: {
                HStack(spacing: 8) {
                    Text(viewModel.currency.flag).font(.system(size: 20))
                    Text(viewModel.currency.code)
                        .font(.system(size: 18, weight: .semibold))
                    Image(systemName: "chevron.down")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Palette.onBackground.opacity(0.6))
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Palette.surface, in: Capsule())
                .foregroundStyle(Palette.onBackground)
            }
            .accessibilityLabel("통화 바꾸기")

            Spacer()

            if viewModel.loading {
                ProgressView().tint(Palette.accent)
            } else {
                Button {
                    Task { await viewModel.refresh() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Palette.onBackground.opacity(0.7))
                }
                .accessibilityLabel("환율 새로 받기")
            }
        }
        .padding(.top, 12)
    }

    private var amountBlock: some View {
        VStack(alignment: .trailing, spacing: 14) {
            Text("\(formatAmountInput(viewModel.amountInput)) \(viewModel.currency.code)")
                .font(.system(size: 34, weight: .medium, design: .rounded))
                .foregroundStyle(Palette.onBackground.opacity(0.55))
                .lineLimit(1)
                .minimumScaleFactor(0.5)

            Text(viewModel.convertedKrw.map { "₩ \(formatKrw($0))" } ?? "—")
                .font(.system(size: 56, weight: .bold, design: .rounded))
                .foregroundStyle(Palette.onBackground)
                .lineLimit(1)
                .minimumScaleFactor(0.4)
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    private var rateLine: some View {
        VStack(alignment: .trailing, spacing: 3) {
            Text(rateMessage)
                .font(.system(size: 15))
                .foregroundStyle(Palette.onBackground.opacity(0.8))
                .multilineTextAlignment(.trailing)

            if let snapshot = viewModel.snapshot {
                Text(
                    "\(formatUpdatedAt(snapshot.fetchedAt)) · \(snapshot.providerLabel)"
                        + (viewModel.errorMessage == nil ? "" : " · 새로고침 실패")
                )
                .font(.system(size: 12))
                .foregroundStyle(Palette.onBackground.opacity(0.45))
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    private var rateMessage: String {
        if let error = viewModel.errorMessage, viewModel.snapshot == nil { return error }
        if viewModel.unsupported {
            return "\(viewModel.currency.code) 환율은 지금 공급처에서 제공하지 않습니다"
        }
        guard let per = viewModel.krwPerUnit else { return "환율을 불러오는 중입니다" }

        let unit = viewModel.currency.displayUnit
        return "\(unit) \(viewModel.currency.code) = \(formatRate(per * Double(unit))) KRW"
    }
}

// MARK: - 키패드

private struct Keypad: View {
    let onDigit: (Character) -> Void
    let onDecimalPoint: () -> Void
    let onBackspace: () -> Void
    let onClear: () -> Void

    private let rows = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], [".", "0", "⌫"]]

    var body: some View {
        VStack(spacing: 10) {
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 10) {
                    ForEach(row, id: \.self) { key in
                        KeypadKey(key: key) {
                            switch key {
                            case ".": onDecimalPoint()
                            case "⌫": onBackspace()
                            default: onDigit(Character(key))
                            }
                        } onLongPress: {
                            if key == "⌫" { onClear() }
                        }
                    }
                }
            }
        }
    }
}

/// 짧게 누르면 입력, 지우기 키는 길게 누르면 전체 삭제.
private struct KeypadKey: View {
    let key: String
    let onTap: () -> Void
    let onLongPress: () -> Void

    var body: some View {
        Button(action: {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            onTap()
        }) {
            // 비율은 배경 도형이 잡는다. 글자에 걸면 세로로 늘어나지 않는다.
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Palette.surface)
                .aspectRatio(1.6, contentMode: .fit)
                .overlay {
                    if key == "⌫" {
                        Image(systemName: "delete.left")
                            .font(.system(size: 22, weight: .medium))
                    } else {
                        Text(key)
                            .font(.system(size: 26, weight: .medium, design: .rounded))
                    }
                }
                .foregroundStyle(Palette.onBackground)
        }
        .buttonStyle(.plain)
        .onLongPressGesture(minimumDuration: 0.4) {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            onLongPress()
        }
        .accessibilityLabel(key == "⌫" ? "지우기, 길게 누르면 전체 삭제" : key)
    }
}

// MARK: - 통화 선택

private struct CurrencyPicker: View {
    let selected: Currency
    let snapshot: RateSnapshot?
    let onPick: (Currency) -> Void

    @State private var query = ""

    private var filtered: [Currency] {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return Currencies.all }
        return Currencies.all.filter {
            $0.code.localizedCaseInsensitiveContains(trimmed)
                || $0.name.localizedCaseInsensitiveContains(trimmed)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { currency in
                Button {
                    onPick(currency)
                } label: {
                    HStack(spacing: 14) {
                        Text(currency.flag).font(.system(size: 24))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(currency.code).font(.system(size: 16, weight: .semibold))
                            Text(currency.name)
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if let rate = snapshot?.krwPerUnit(currency.code) {
                            Text(formatRate(rate * Double(currency.displayUnit)))
                                .font(.system(size: 14))
                                .foregroundStyle(.secondary)
                        }
                        if currency.code == selected.code {
                            Image(systemName: "checkmark")
                                .foregroundStyle(Palette.accent)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .listStyle(.plain)
            .searchable(text: $query, prompt: "통화 이름이나 코드로 검색")
            .navigationTitle("통화 선택")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    ContentView().preferredColorScheme(.dark)
}
