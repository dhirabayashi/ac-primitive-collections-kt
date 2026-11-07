# ac-primitive-collections-kt

AtCoder の Kotlin 向けに、ボクシングなしで使えるプリミティブ型コレクションを作るプロジェクトです。

**開発中です。今は `IntList` だけが動きます。**

## 予定している内容

- 可変長配列、Deque、優先度付きキュー、HashSet / HashMap、TreeSet / TreeMap、多重集合
- `kth` / `rank` などの順序統計
- `[]`、`in`、`+` などの Kotlin の演算子
- 比較関数付きソート、argsort
- 1 ファイルに結合して提出するためのバンドラ

詳しくは [計画書](docs/PLAN.md) を参照してください。

## 対象環境

現行の AtCoder ジャッジに合わせています。実測値は [ジャッジ環境の実測記録](docs/judge-env.md) にあります。

- Kotlin 2.2.10
- JDK 23

当面は 1 ファイルに結合して提出します。次回の言語アップデートで、ジャッジへのインストールを提案する予定です。

## ビルド

```sh
./gradlew check koverVerify   # テスト、ボクシング検査、カバレッジなど
./gradlew generateSources     # templates/ を変更したら実行して generated/ を更新
./e2e/run-e2e.sh              # ジャッジ再現環境での E2E(Docker が必要)
./gradlew pitest              # 変異テスト(数分かかる)
```

JDK 23 が必要です。

## 提出

`Main.kt` では `import ac_primitive_collections.*` と書きます。AtCoder にはこのライブラリが入っていないので、そのまま提出すると CE になります。バンドルしたものを提出します。

`Main.kt` のあるフォルダで次を実行すると、提出するコードがクリップボードに入ります。あとは AtCoder に貼り付けるだけです。

```sh
bundle -p     # 練習(過去問など)。今の作業ツリーから作る
bundle        # コンテスト。チェックアウトの状態にかかわらず、push 済みの最新タグから作る
```

どのフォルダからでも使えるように、最初に 1 回だけリンクを作ります。

```sh
ln -s "$PWD/bundle" ~/.local/bin/bundle
```

## 生成 AI ルールについて

このリポジトリのコードは、生成 AI を使って作成しています。

[AtCoder 生成 AI 対策ルール](https://info.atcoder.jp/entry/llm-rules-ja) では、生成 AI で作成したコードを ABC / ARC / AGC の開催中に使う場合の条件が定められています。作成者が自分か他人かを問わず適用されるので、**このリポジトリのコードをコピーして使う場合も同じです**。

- コンテスト開始前に公開されているコードを使う
- 提出に参照 URL を添付する

コンテストの開催中に、生成 AI でこのライブラリを修正して使うことはできません。

最新の条件は、ルールの原文で確認してください。

## サポート

作者が AtCoder で使うためのものです。自由に使ってかまいませんが、サポートや互換性の保証はしません。

## ライセンス

[CC0-1.0](LICENSE)
