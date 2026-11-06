# ac-primitive-collections-kt

AtCoder 用の、ボクシングしない Kotlin プリミティブコレクションです。所有者本人が使うためのもので、最終目標は次回の言語アップデートでのジャッジへのインストールです。それまでは貼り付けで提出します。

## 最初に読むもの

- 設計と計画: `docs/PLAN.md`。現在地は「Phase 2 の進捗」、未確認事項は §15 にあります。
- AtCoder での実測: `docs/judge-env.md`
- 生存変異の確認: `docs/mutation-review.md`

## コードの場所

- `templates/`、`test-templates/`: 編集するのはここです。`generated/` を直接編集してはいけません。
- `generated/`: 生成物をコミットします。ビルドはここから行います。生成する型は `build-logic/src/main/kotlin/codegen/Codegen.kt` の `GENERATED_TYPES` と `GENERATED_PAIRS` で決まります。今は、単一型のテンプレートが Int だけ、ペアのテンプレートが §5.1 の全組み合わせ(K: Int, Long, Double × V: Int, Long, Obj)です。`MinHeap` を名前に含むテンプレートからは `MaxHeap` も生成されます。テンプレート自体のコンパイル検査はないので、他の型は展開して初めてコンパイルされます。
- `e2e/usage/*/`: `Main.kt`、`expected.txt`、任意の `input.txt` を置くと E2E の対象になります。
- `e2e/experiments/`: AtCoder に貼って測る比較用のコードです。

## 検証

JDK は Gradle が 23 を選びます(`gradle/gradle-daemon-jvm.properties`)。

```sh
./gradlew generateSources                    # templates/ を変更したら実行
./gradlew check koverVerify :build-logic:test --continue
./gradlew pitest                             # 閾値 90%。生存変異は docs/mutation-review.md で確認
./e2e/run-e2e.sh                             # Docker が必要。ジャッジ再現環境で貼り付けと jar の両方
reuse lint                                   # 未導入なら venv に pip install reuse
```

ボクシング、確保量、分岐カバレッジ 100%、メソッド長、禁止パターンは `check koverVerify` に含まれます。JMH の値は arm64 なので、判断には使いません(§7.6)。

## 提出

- 解答用プロジェクトは `/Users/daiki/IdeaProjects/atcoder`(git 管理外)です。このリポジトリを `includeBuild` しています。提出コードは `./submit` でクリップボードに入ります。
- `./submit -p` は練習用で、今の作業状態からバンドルします。
- `./submit`(コンテスト用)は、このリポジトリが **push 済みのタグの位置**で、`generated/main` と `src/main` に変更がないときだけ動きます。公開タグは `v0.1.0`(`IntList` のみ)です。開発中のブランチにいると失敗するので、コンテストの前に `git checkout v0.1.0` が必要です。「最新の push 済みタグからバンドルする」改善案は未着手です。
- コードテスト用のファイルは次のように作ります。

  ```sh
  ./bundle -p -o <出力先>.kt e2e/experiments/compare-priorityqueue/Main.kt
  ```

## ルール

- 依頼するときは、理由と、実行するコマンド、開くページ、提出するものを具体的に書きます。
- AtCoder での手作業は、判断が変わるときだけ、最小の回数で依頼します。先に Docker のジャッジ再現環境(`e2e/judge`)で確認します。
- ジャッジの判断は AtCoder の実測で行います。手元や Docker(エミュレーション)の値をジャッジの値として報告してはいけません。
- コミット、push、タグは許可を得てから行います。タグは参照 URL の根拠なので、付け替えも削除もしません。
- `gh` は入っていません。PR の作成は所有者が行います。作業はブランチで行い、`main` に直接 push しません。
- 文書は日本語で書き、結果と決定を `docs/` に残します。
