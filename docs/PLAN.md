# ac-primitive-collections-kt 計画書

- 作成日: 2026-11-05
- 状態: Phase 2(線形構造とソート)の途中。公開タグは `v0.3.0`。進捗は §11
- 対象: AtCoder の Kotlin 言語環境で使う、プリミティブ型特化コレクションライブラリ

---

## 1. 目的と成功条件

### 1.1 本来の目的

AtCoder の Kotlin 提出で、**ボクシングが原因の TLE / MLE をなくすこと**。

クラスをそろえるだけでは目的を達成できません。典型的なボトルネックを、書きやすいコードのまま解消できる必要があります。実際にボクシングが起きやすいのは次の箇所です。

| よくあるコード | 問題 | 本ライブラリでの解決 |
|---|---|---|
| `PriorityQueue<Pair<Long, Int>>`(Dijkstra) | `Pair` と `Long` / `Int` を毎回確保 | `LongIntMinHeap` |
| `Array(n) { mutableListOf<Int>() }`(隣接リスト) | 要素ごとのボクシング、参照の間接化 | `CsrGraph`、`IntList` |
| `HashMap<Int, Int>`(カウンタ) | キーと値のボクシング、Node の確保 | `IntIntHashMap.addTo(k, 1)` |
| `TreeMap<Int, Int>` / `TreeSet<Int>` | 同上。さらに k 番目や rank の取得ができない | `IntIntTreeMap`、`IntTreeSet.kth()` |
| `indices.sortedBy { a[it] }` | `List<Int>` と比較のボクシング | `a.argsortBy { it }` |
| `HashSet<Pair<Int, Int>>`(グリッド) | `Pair` の確保 | `IntPair` + `LongHashSet` |

### 1.2 成功条件(計測できる形で定義)

| # | 条件 | 検証方法 |
|---|---|---|
| S1 | ライブラリ(§7.3 の例外を除く)と代表的な利用コードの**どちらのバイトコードにもボクシング呼び出しがない** | ASM による静的検査(§7.3) |
| S2 | ウォームアップ後のホットパス(§7.3 で定義)で確保量がゼロ(拡張時を除く) | スレッド確保量の計測テスト(§7.3) |
| S3 | **現行ジャッジで、1 ファイルに貼って提出できる**(これが主な利用形態) | バンドラ(§8)、AtCoder への実提出 |
| S4 | バンドル後のコードが**現行ジャッジと同一のコマンド**でコンパイル・実行でき、テスト済みの動作と一致する | E2E(§7.5)、AtCoder コードテスト |
| S5 | `java.util` より速い | AtCoder コードテストで、同じファイルに書いた両方の実装を比較(§7.6)。fastutil と HPPC との比較は Linux x64 CI での参考値とする |
| S6 | 生成 AI ルールとライセンスの両方に適合する | §9、§10 のチェックリスト |
| S7 | 差分テストと変異テストの基準を満たす | §7 の品質ゲート |
| S8 | リリースごとに、ジャッジへのインストールを提案できる状態を保つ | jar 方式の E2E、インストール手順のリハーサル(§1.4) |

### 1.3 スコープ外

- ACL(ac-library-java)と重複するもの: セグメント木、DSU、FFT など
- スレッドセーフ性: ジャッジでは並列化しても実行時間が縮まないため(§2.1)
- `List<Int>`、`Iterable<Int>` などの実装(§4.6 の理由により意図的に行わない。変換は提供する)
- Kotlin Multiplatform / Native: ジャッジは JVM のみ
- 高速入出力: 有用だが別テーマ。v1 以降に別モジュールとして検討
- **作者本人が AtCoder で使う以外の用途**: 他者の利用は妨げないが、サポート、互換性の保証、他者向けの文書整備はしない
- Maven Central などのパッケージレジストリへの公開
- Java など Kotlin 以外の言語からの利用: 手元で Kotlin stdlib をクラスパスに通せば動く範囲で使えるが、動作確認やサポートはしない。ジャッジへのインストールも Kotlin のエントリのみを対象とする

### 1.4 利用形態の目標

**最終目標は、次回の言語アップデートで提案してジャッジにインストールし、`import` だけで使えるようにすることです。** 貼り付けは不便なので、それまでの暫定手段と位置づけます。

| 時期 | 利用形態 | 対象環境 |
|---|---|---|
| 当面 | バンドラで貼り付け | 現行ジャッジ(Kotlin 2.2.10、JDK 23.0.2) |
| 次回アップデート以降(目標) | `import` のみ | その時点の環境(時期とバージョンは不明) |

この目標のため、最初から次を守ります。

- **移行時にコードをできるだけ書き換えない:** 利用者のコードは `import` で書き、貼り付け時はバンドラが展開します。導入後はバンドラを通さずに提出します。貼り付けと `import` では名前の解決規則が違い、完全な同一性は保証できないので、差が出るケースはバンドラで検出します(§8.3)。
- **jar も本番品質にする:** 全ゲートの対象とし、リリースごとに公開します。実行時依存なし、Kotlin stdlib は同梱しません。
- **提案の材料を継続的に用意する:** インストールスクリプトの案とリハーサルをリポジトリと CI で維持し、jar のサイズ、コンパイル時間、起動時間への影響も毎リリース記録します。
- **特定のバージョンに縛られない:** JDK の内部 API、プレビュー機能、Kotlin の実験的機能に依存しません。これにより、新環境でも再ビルドと再検証で済むようにします。将来のバージョン向けの先回りの作業まではしません。

---

## 2. 調査で確認した前提

### 2.1 ジャッジ環境(公式の言語一覧と install TOML で確認)

| 項目 | 内容 |
|---|---|
| 言語 | `Kotlin (Kotlin/JVM 2.2.10)` |
| JDK | OpenJDK 23.0.2 |
| コンパイル | `kotlinc ./Main.kt -cp ac_library.jar:kotlinx-collections-immutable-jvm.jar -include-runtime -jvm-target 23 -d ./Main.jar` |
| 実行 | `java -Xss{min(メモリ制限MB,1024)}M -DONLINE_JUDGE=true -cp ac_library.jar:kotlinx-collections-immutable-jvm.jar:Main.jar MainKt` |
| ヒープ指定 | **`-Xmx` なし**(JVM の既定値に従う)。実測で `maxMemory()` は **922 MiB** |
| CPU、GC(コードテストで実測) | プロセッサ **1 個**、**Serial GC**、Xeon Platinum 8488C(AVX-512 有効)。詳細は `docs/judge-env.md` |
| 導入済みライブラリ | ac-library-java v2.0.0(CC0-1.0)、kotlinx-collections-immutable 0.4.0(Apache-2.0) |
| 時間計測 | **実時間と CPU 時間の大きい方**。並列化しても短くならない |
| MLE | 超過時点で強制終了(新ジャッジ) |

### 2.2 ライブラリ導入の手続き

- インストール依頼は言語アップデートの枠組みで、公式 Discord の「インストール依頼」チャンネルから行います。依頼内容は基本的にそのまま機械的に実行され、**導入は保証されません**。
- 公式のお問い合わせページには、言語の追加や更新は個別対応しないとあります。
- **現行ジャッジには導入できません。** 導入できるのは次回の言語アップデートで、時期は未定(数年先の可能性あり)です。その時点の Kotlin / JDK のバージョンも分かりません。
- したがって、当面の開発は現行ジャッジ(§2.1)に合わせ、将来のバージョンを先取りした対応はしません。

### 2.3 生成 AI ルール(20261014 版、ABC / ARC / AGC に適用)

事前に生成 AI で作成したコードは、次の両方を満たせば使えます。

1. コンテスト開始前に Web 上に公開する(第三者が内容とタイムスタンプを確認できること)
2. コピーして使うときは参照 URL を添付する

他人が AI で作成したコードにも同じ条件が適用され、**知らずに出典なしでコピーした場合も違反**です。難読化も禁止されています。バイナリ化して埋め込むコードは、2026/10/20 以降、元コードを確認できるようにする必要があります。

### 2.4 既存ライブラリ

| ライブラリ | jar サイズ(Maven Central で実測) | 評価 |
|---|---|---|
| fastutil 8.5.15 | 23.7 MB | 豊富だが巨大で貼り付け不可。`java.util` 互換の API はボクシング経路を含む |
| Eclipse Collections 11.1.0 | 10.5 MB(別途 API jar が必要) | 同上 |
| HPPC 0.10.0 | 1.5 MB | 軽量。ただしツリーや順序統計はない |

いずれも Kotlin 向けの API ではなく、順序統計付きのツリーやペアヒープもありません。**自作する価値はありますが、性能の比較基準として fastutil と HPPC を使います。**

---

## 3. 主要な設計判断

| 判断 | 理由 | 詳細 |
|---|---|---|
| 自然順ソートは `Arrays.sort` に委譲 | JDK のプリミティブソートは既に特化されており、ジャッジの実測で基数ソートより速い。比較関数付きソートと argsort に注力する | §6 |
| 貼り付け提出から始め、次回の言語アップデートでインストールを提案 | 現行ジャッジには導入できない | §1.4、§8 |
| ボクシングの不在を CI で検証 | `Int?`、ジェネリクス、非 inline ラムダのどれか 1 つで混入し、目視では防げない | §7.3 |
| テストの品質を測定可能な基準で定義 | カバレッジ、変異スコア、差分テスト、実問題で判定し、残るリスクも明記する | §7.1 |
| 利用頻度に基づく型行列 | 全組み合わせは数百クラスになり、大半は使われない | §5.1 |
| 生成コードのコミットと参照 URL の自動付与 | 生成 AI ルール(§2.3)の条件を満たし、URL だけで内容を確認できるようにするため | §9 |
| ビルドの Kotlin と JDK をジャッジに合わせる | コンパイラが違うとバイトコードも変わる | §11 Phase 0 |
| キー + ペイロードのヒープ、インデックス付きヒープ | Dijkstra で頻繁に起きるボクシングを解消する | §5.5 |
| 順序統計(`kth`、`rank`)、多重集合 | `TreeSet` にない機能で、AtCoder での需要が高い | §5.7 |
| `IntPair`、CSR グラフ、ビットセット、FastSet、座標圧縮 | 周辺で起きるボクシングや `java.util` の機能不足を解消する | §5.8 |
| `Int?` を返さない API | `Int?` はボクシングする | §4.3 |
| MinHeap / MaxHeap を個別に生成 | 符号反転で最大ヒープを作ると `Int.MIN_VALUE` で壊れる | §5.5 |
| メモリ使用量と CPU 時間の管理 | GC や JIT の CPU 時間も実行時間に計上され、MLE は即時終了になる | §7.6 |

---

## 4. 設計原則

### 4.1 オーバーヘッドなしで許可する抽象化

| 手段 | 判定 | 備考 |
|---|---|---|
| コード生成 | ○ | 実行時コストなし。重複排除の主手段 |
| `inline fun` + ラムダ | ○ | 下記の制約に注意 |
| final クラス、static な関数 | ○ | |
| `@JvmInline value class`(非ジェネリック文脈のみ) | ○ | `IntPair` など。`List<IntPair>` にするとボクシングする |
| 非ジェネリックな `fun interface`(`IntComparator`) | △ | ボクシングはしないが仮想呼び出しになる。比較関数付きの API に限定 |
| `T`、`Comparable<T>`、`(Int) -> Int` の保持 | × | ボクシングする |
| `Iterable<Int>`、`Collection<Int>`、`Sequence<Int>`、`Pair` | × | ボクシングする |
| 共通インターフェースを介したホットパス | × | v1 では定義しない |

### 4.2 `inline` の落とし穴

- **再帰できません。** inline 関数は再帰呼び出しができないため、ソートなどをそのまま inline 化できません。
- **巨大メソッドは JIT されません。** `DontCompileHugeMethods=true`(既定)のため、8000 バイトコードを超えるメソッドはコンパイルされず、極端に遅くなります。大きな inline を展開すると、利用者の `main` がこの上限を超えるおそれがあります。
  - 対策: inline 関数は薄く保ち、本体は通常の関数に分離します。`forEach` のような短いものに限定し、サイズを CI で監視します(§7.3)。
- **public inline は内部を公開します。** 参照先は `@PublishedApi internal` にする必要があります。
- **クラスには通常のメソッドも残ります。** ラムダを受け取る `inline fun` の本体には `Integer.valueOf` と `Function1.invoke` が含まれます(Kotlin 2.2.10 で確認)。Kotlin から呼ぶ限り展開されるので実行はされませんが、ボクシング検査では別扱いが必要です(§7.3)。

### 4.3 「値がない」の表現

`Int?` はボクシングするため、`*OrNull` は**提供しません**。統一パターンは次のとおりです。

```kotlin
// 1. 例外(空・不在はバグとして扱う場合)
list.last(); set.floor(x)
// 2. 既定値
map.getOrDefault(k, -1); set.floorOrDefault(x, -1)
// 3. inline ラムダ
map.getOrElse(k) { compute() }; set.floorOrElse(x) { return }
// 4. 存在確認とカーソル(二重探索を避ける)
if (k in map) { /* ... */ }
val c = map.findSlot(k); if (c >= 0) map.valueAt(c)
```

**Map の `map[k]` は例外にせず、キーがなければ値型のゼロ値を返します。** HashMap と TreeMap の全組み合わせで共通です。

| 値の型 | キーがないときの `map[k]` |
|---|---|
| `Int`、`Long` | `0` |
| `Double` | `0.0` |
| `Obj`(`V`) | `null`(戻り値の型は `V?`。参照型なのでボクシングは発生しない) |

- `map[k]` を読んでもエントリは追加されません(C++ の `operator[]` とは異なります)。
- 値 0 が入っている場合とキーがない場合は `map[k]` では区別できないので、KDoc で明記し、区別が必要なら `k in map`、`containsKey`、`findSlot` を使うよう案内します。

同じ理由で、`put` は古い値を返しません。`remove(k): Boolean`、`removeOrDefault(k, d)`、`putIfAbsent(k, v): Boolean` に分けます。

### 4.4 `Double` の意味論

テンプレートの共通化で最もバグが入りやすい箇所です。

| 演算 | IEEE(`<`、`==`) | 全順序(`compareTo`、`equals`) |
|---|---|---|
| `NaN == NaN` | false | true |
| `0.0 == -0.0` | true | false |

本ライブラリは**全順序に統一**し、`java.util` および `Arrays.sort` と揃えます。テンプレートでは `<` や `==` を直接書かず、型ごとに展開されるマクロ(`K_LT`、`K_EQ`)を使います。

### 4.5 整数オーバーフロー対策のカタログ

以下は CI の禁止パターン検査と境界値テストの対象です。

| 箇所 | NG | OK |
|---|---|---|
| 中点 | `(lo + hi) / 2` | `(lo + hi) ushr 1`(Int の添字)、`lo + (hi - lo) / 2` |
| 比較 | `a - b` | `a.compareTo(b)` |
| 容量拡張 | `cap * 2`、`cap + cap / 2` | Long で計算し上限で丸める共通関数 `nextCapacity` |
| 上限 | `Int.MAX_VALUE` の配列を確保 | 上限 `Int.MAX_VALUE - 8`。超過時は明示的な例外 |
| 合計 | `Int` の和を `Int` で返す | `sum(): Long`。`Long` の和はラップすると明記 |
| 長さ | `(hi - lo).toInt()`、`hi - lo + 1` | Long で計算し、範囲外なら例外 |
| シフト | `1 shl i`(i ≥ 32) | `1L shl i`。シフト量を検証 |
| 負の剰余 | `h % n` | `h and (n - 1)`、`Math.floorMod` |
| 反転 | `-x`、`abs(x)` | Max 版を生成する。`Int.MIN_VALUE` で壊れるため |
| パック | `(a.toLong() shl 32) or b.toLong()` | 下位側は `and 0xFFFFFFFFL`。符号拡張対策 |
| 自己参照 | `list.addAll(list)` | サイズを先に確定、または一時コピー |
| 範囲引数 | `lo > hi` を未検証 | 全 API で検証 |
| 添字 | capacity 内なら配列の境界検査を通過してしまう | `size` に対して明示的に検査 |

### 4.6 標準コレクションとの相互運用

**方針: 変換は用意する。型レベルの互換性(`List<Int>` などの実装)は持たせない。**

| レベル | 内容 | 判定 |
|---|---|---|
| 1. 変換(コピー) | `toList()`、`toIntList()` など | **提供** |
| 2. ビュー | `asList(): List<Int>`(別クラスのラッパー) | v1 では提供しない。実際に必要になったら追加 |
| 3. 型レベルの互換 | `IntList : List<Int>` | **提供しない** |

**型レベルの互換を持たせない理由**(Kotlin 2.2.10 + JDK 23 でバイトコードを確認済み)

`AbstractList<Int>` を継承したクラスと、継承しないクラスを比較しました。

| 利用コード | `List<Int>` を実装 | 実装しない |
|---|---|---|
| `get` のシグネチャ | **`Integer get(int)`** | `int get(int)` |
| `l[i]` | **`Integer.intValue` を呼ぶ** | ボクシングなし |
| `for (x in l)` | **`java.util.Iterator` + `Number.intValue`** | `IntIterator.nextInt()` |
| `l.filter { }.sum()` | **ボクシングしたままコンパイルが通る** | コンパイルエラー(専用メンバーを使う) |

- `l[i]` のような基本操作まで、実装するだけでボクシングする経路に変わります。本ライブラリの目的と両立しません。
- 標準の拡張関数がすべて使えるようになるので、専用メンバーがない操作は**警告なしで**ボクシングします。実装しなければコンパイルエラーになり、ボクシングに気づけます。

**提供する変換**(いずれもコピーで、ビューではありません)

| 方向 | API |
|---|---|
| 標準コレクションへ | `IntList` / `IntDeque` → `toList()`、`toMutableList()`。`IntHashSet` / `IntTreeSet` → `toSet()`、`toMutableSet()`。`IntIntHashMap` / `IntIntTreeMap` → `toMap()`、`toMutableMap()`。ヒープ → `toUnorderedList()`、`toSortedList()` |
| 標準コレクションから | `Iterable<Int>.toIntList()`、`Iterable<Int>.toIntHashSet()`、`Iterable<Int>.toIntTreeSet()`、`Map<Int, Int>.toIntIntHashMap()`、`Map<Int, Int>.toIntIntTreeMap()` など。`addAll(Iterable<Int>)` |
| 配列・範囲から(ボクシングなし) | `IntArray.toIntList()`、`IntProgression.toIntList()`、`addAll(IntProgression)` など。`IntProgression` は `1..n`、`1..n step 2`、`n downTo 1` のすべてを受ける。受け手が `IntRange` だと `step` と `downTo` は `Iterable` 版に解決され、警告なしでボクシングする。ACL は配列を受け取るので、`toIntArray()` と合わせて最も重要な経路 |

**`Iterable` を受ける API には、範囲を受ける版を必ず並べます。** 対象は `Int`、`Long`、`Char` です(`toIntHashSet` など、これから作るものも含みます)。ボクシングはライブラリの中で起きるので、利用コードの検査では見えません。`SourceRulesTest` の `iterableOverloadsHaveProgressionOverloads` が、同じファイルに範囲版がないことを検出します。

順序の仕様:

- `TreeSet` / `TreeMap` の変換は昇順を保ち、`LinkedHashSet` / `LinkedHashMap` を返します。
- `HashSet` / `HashMap` の変換の順序は不定です。

**同じ型どうしの `equals` のみ** `true` を返します。`IntList` と `List<Int>` を比較したい場合は `toList()` で揃えます。`equals` の対称性を保つためです。

### 4.7 可変と不変

**全コレクションを可変の final クラス 1 種類にします。**

| 案 | 判定 | 理由 |
|---|---|---|
| 可変のみ(`IntList`) | **採用** | クラス数とバンドルサイズを増やさず、呼び出しも直接のまま |
| 読み取り専用の型を追加(`IntList` + `MutableIntList`) | 不採用 | 継承やインターフェースで呼び出し経路が増え、クラスも倍増する。共有バグを防ぐ利点は、短い単一ファイルの提出では小さい |
| 不変クラス | 不採用 | 更新ごとの全コピーは競技用途に合わない。不変にしたいだけなら、変更しなければ足りる |
| 永続データ構造 | スコープ外 | 別の種類のデータ構造で、必要なら別途検討 |

- 命名は Kotlin 標準(`List` は読み取り専用、`MutableList` は可変)と異なり、`IntList` が可変です。標準の `IntArray` が可変であることに合わせます。
- 独立したコピーが必要なときは `copy()` を使います。`toList()` などの変換もコピーを返します(§4.6)。

### 4.8 Kotlin らしい書き方

Kotlin の演算子と慣用表現を一通りサポートします。**標準コレクションから書き換えるときに、なるべく同じ書き方で済むようにします。** いずれもボクシングなしで実装し、§7.3 の検査対象にします。

| 記法 | 対象 | 意味 |
|---|---|---|
| `a[i]`、`a[i] = v` | List、Deque、FixedBitSet | 添字アクセス |
| `m[k]`、`m[k] = v` | Map | 取得(不在時はゼロ値)、設定 |
| `a[i] += x`、`a[i]++`、`m[k] += x`、`m[k]++` | 同上 | `get` + `set` に展開される |
| `x in c`、`x !in c` | List、Deque、Set、Multiset、FixedBitSet、FastSet | 要素の存在確認 |
| `k in m`、`k !in m` | Map | キーの存在確認 |
| `a + x`、`a + b` | List、Deque、Set、Multiset | 要素や別のコレクションを加えた**新しい**コレクション |
| `m + other` | Map | 同上(同じキーは右側を優先) |
| `a - x`、`a - b` | List、Deque、Set、Multiset | 除いた**新しい**コレクション(List の `- x` は標準と同じく最初の 1 個のみ) |
| `m - k` | Map | キーを除いた新しい Map |
| `a += x`、`a -= x` | 上記の可変版 + ヒープの `+=` | その場で変更 |
| `s1 union s2`、`intersect`、`subtract` | Set | 集合演算 |
| `b1 and b2`、`or`、`xor`、`shl`、`shr`、`inv()` | FixedBitSet | ビット演算 |
| `for (x in c)` | List、Deque、Set、FixedBitSet、FastSet | 反復 |
| `for ((k, v) in m)` | Map | 反復(カーソル方式、下記) |
| `val (a, b) = pair` | `IntPair` | 分解 |
| `val (a, b) = list` | List | 先頭要素の分解(`component1`〜`component5`) |

**慣用的な名前も標準に揃えます**

- プロパティ: `size`、`indices`、`lastIndex`
- 判定: `isEmpty()`、`isNotEmpty()`
- 生成: `intListOf(1, 2, 3)`、`IntList(n) { it * 2 }`、`buildIntList { add(1) }`、`intHashSetOf(...)`、`intTreeSetOf(...)`
- 新しいコレクションを返す: `sorted()`、`sortedDescending()`、`reversed()`、`distinct()`、`take()`、`drop()`、`map {}`、`filter {}`
- 文字列: `joinToString()`、`toString()`(`[1, 2, 3]`、`{1=2}`。標準と同じ形式)

**意図的に提供しないもの**(いずれもボクシングまたは `Pair` が必要になるため)

- `*OrNull`(`firstOrNull` など)。代わりは §4.3
- `withIndex()`。代わりは `forEachIndexed`
- `mapOf(1 to 2)` 形式の生成、`m += (k to v)`。代わりは `m[k] = v`
- `keys`、`values`、`entries` プロパティ。代わりは `forEachKey`、`keysToArray()` など

**実験で確認した点**(Kotlin 2.2.10 + JDK 23)

| 項目 | 結果 |
|---|---|
| `[]`、`in`、`+`、`+=`、`m[k] += 1`、`m[k]++`、`IntPair` の分解 | ボクシングなし |
| `for ((k, v) in m)` | `component1(): Int` などを直接呼び、ボクシングなし |
| `var a` に対する `a += x`(`plus` と `plusAssign` の両方がある場合) | **コンパイルエラー**(候補が曖昧)。`val a` なら問題なし |

**注意点**

- `+=` の曖昧さのエラーは標準の `MutableList` と同じ挙動です。`plus` を残すために受け入れます。可変コレクションは `val` で宣言するか、`add` を使います。
- `a + x` は毎回全コピーするので、ループで `a = a + x` と書くと O(n²) になります。KDoc に明記し、ループでは `+=` を使います。
- `m[k] += 1` は探索が 2 回走ります。ボクシングはありませんが、ホットパスでは 1 回で済む `m.addTo(k, 1)` を使います。性能差は JMH で確認します。
- `for ((k, v) in m)` のカーソルは 1 つのオブジェクトを再利用します。エントリをループの外に持ち出すと値が変わります。
- `indices` は `0 until size` ではなく `IntRange(0, size - 1)` で実装します。`0 until size` はウォームアップ後も確保が残りましたが、後者は 0 でした(arm64 で計測。`e2e/experiments/IndicesAllocation.kt`)。
- `for (x in list)`、`for (i in list.indices)`、`for ((k, v) in m)` の確保の除去は JIT のエスケープ解析に依存し、**実行の初期には確保が発生します**。確実に確保を避けたいときは `for (i in 0 until list.size)` または `list.forEach {}` を使います。

---

## 5. 提供するコレクション

### 5.1 型行列

K はキー、V は値です。ソース上で約 100 クラスを見込みます。イテレータやカーソルなどの補助クラスを含めると、class ファイルはさらに増えます。jar の予算(§7.6)に収まるかは Phase 2 で実測します。

| コレクション | 型 | クラス数 |
|---|---|---|
| `List` | Int, Long, Double, Char | 4 |
| `SubList` | 同上 | 4 |
| `Deque` | 同上 | 4 |
| `MinHeap` / `MaxHeap` | Int, Long, Double × 2 | 6 |
| ペアヒープ `KVMinHeap` / `KVMaxHeap` | K: Int, Long, Double × V: Int, Long, Obj × 2 | 18 |
| `IndexedMinHeap` / `IndexedMaxHeap` | K: Int, Long, Double × 2 | 6 |
| 比較関数付きヒープと比較関数インターフェース(`IntComparator` など) | Int, Long, Double × 2 | 6 |
| `HashSet` | Int, Long, Double, Char | 4 |
| `HashMap` | K: Int, Long, Char, Double, Obj × V: Int, Long, Double, Obj(Obj-Obj を除く) | 19 |
| `TreeSet` | Int, Long, Double, Char | 4 |
| `TreeMultiset` | Int, Long, Double, Char | 4 |
| `TreeMap` | K: Int, Long, Double, Char × V: Int, Long, Double, Obj | 16 |
| 単独クラス | `FixedBitSet`、`FastSet`、`CsrGraph`、`IntPair` など | 数個 |

- `UInt` / `ULong`: v1 では対象外です。生成器の型定義は拡張可能にしておき、需要があれば追加します。
- **`Float`、`Short`、`Byte`、`Boolean` のコレクションは作りません。** 所有者が AtCoder で使わないためです。必要になったら `IntList` や `FixedBitSet` などで代用します。`Boolean` は Map の値にもしません(`HashSet` で表せます)。
- **`Char` のヒープも作りません。** 文字をヒープで扱う場面はまれで、`IntMinHeap` に文字コードを入れれば足ります。

### 5.2 共通操作

すべてのコレクションに次を用意します: `size`、`isEmpty()`、`isNotEmpty()`、`clear()`、`copy()`、`toXArray()`、`equals`、`hashCode`、`toString`、`appendTo(StringBuilder, separator)`、初期容量つきコンストラクタ、`intListOf(vararg)` 形式の生成関数、配列からの構築。演算子と慣用的な名前は §4.8 に従います。

**ペアヒープは例外です。** `toXArray`、`appendTo`、生成関数、配列からの構築は持ちません。キーと値の組では `toXArray` の形が定まらず、Dijkstra などの用途でも使わないのに、18 クラスと提出コードを増やすためです。

標準コレクションとの変換(`toList()` など、§4.6)にはボクシングが伴うので、`@BoxingApi` を付けて静的検査の例外にします。

### 5.3 List(`IntList`)

| 分類 | 操作 |
|---|---|
| 要素 | `get`、`set`(operator)、`first`、`last`、`firstOrDefault`、`lastOrDefault`、`firstOrElse`、`lastOrElse` |
| 追加 | `add`、`+=`、`addAll(IntArray / IntList / 部分範囲)`、`insert(i, v)`、`insertAll` |
| 削除 | `removeAt`、`removeLast`、`removeFirst`、`removeValue`、`removeRange`、`swapRemove`、`removeIf {}`、`truncate(n)` |
| 容量 | `ensureCapacity`、`trimToSize`、`resize(n, fill)`、`capacity` |
| 検索 | `contains`、`indexOf`、`lastIndexOf`、`binarySearch`、`lowerBound`、`upperBound`、`indexOfFirst {}`、`indexOfLast {}` |
| 並べ替え | `sort`、`sortDescending`、`sort(from, to)`、`sortWith(IntComparator)`、`sortBy {}`、`sortByLong {}`、`sortByDouble {}`、`argsort`、`argsortBy {}`、`argsortByLong {}`、`argsortByDouble {}`、`reverse`、`shuffle(Random)`、`swap`、`rotate`、`fill`、`dedupe`(隣接重複の除去)、`nextPermutation` |
| 高階 | `forEach`、`forEachIndexed`、`map`、`mapToLong` などの型変換(§5.3.3)、`filter`、`any`、`all`、`none`、`count`、`fold`(§5.3.2)、`sumOf`(§5.3.2) |
| 集計 | `sum`(`Char` にはなし)、`min`、`max`、`minOrDefault`、`maxOrDefault`、`indexOfMin`、`indexOfMax` |
| 反復 | `operator iterator()`(`kotlin.collections.IntIterator` を返す)、`indices`、`slice`、逆順 `forEachReversed` |

#### 5.3.1 `subList`(`IntSubList`)

`list.subList(from, to)` は、`IntList` の範囲 `[from, to)` を指す**ビュー**を返します。`List<Int>` ではなく専用の final クラスなので、ボクシングはしません。コピーが欲しいときは `slice(from, to)` を使います。

| 分類 | 操作 |
|---|---|
| 要素 | `[]`、`[] =`、`first`、`last`、`size`、`indices`、`in` |
| 変更(親に反映) | `sort`、`sortDescending`、`sortWith`、`sortBy {}`、`sortByLong {}`、`sortByDouble {}`、`reverse`、`shuffle`、`fill`、`swap`、`rotate`、`clear()`(親から範囲を削除) |
| 検索・集計 | `indexOf`、`binarySearch`、`lowerBound`、`upperBound`、`sum`(`Char` にはなし)、`min`、`max`、`count {}`、`any {}`、`all {}` |
| 反復 | `for (x in sub)`、`forEach`、`forEachIndexed` |
| 変換 | `subList`(入れ子は親を直接指すように平坦化)、`toIntList()`、`toIntArray()`、`toList()`、`joinToString()`、`contentEquals(IntList / IntSubList)` |

**動作の仕様**

- **長さは固定です。** `add` や `removeAt` は持ちません(例外は `clear()` のみ)。`list.subList(a, b).clear()` は Java でよく使う書き方なので対応します。
- **親の構造変更を検出します。** 親に `add` や `removeAt` などを行った後でビューを使うと例外になります。ずれた範囲を読んで黙って誤答になるのを防ぐためです。`set` など構造が変わらない変更は検出せず、ビューにも反映されます。
- **反復中のビューの `clear()` も検出します。** ラムダや for-in の本体で `clear()` を呼ぶと例外になります。黙って止めると、`all` が 1 要素しか見ずに `true` を返すなどの誤答になるためです。`List` の for-in も、`hasNext` で同じように検出します。
- **反復の検査は、1 要素につき 1 回と、終了時の 1 回です。** すべてのコレクションの `forEach` とイテレータに適用します。イテレータでは要素ごとの検査を `next` が行い、`hasNext` は `false` を返すとき(黙って止まりうる唯一の場所)だけ検査します。ビューは開始時の親の `modCount` と比べます。ビューからの変更(`clear` を含む)も親の `modCount` を進めるので、1 回の比較で両側の変更を検出できます。`main` と比べると、`List` の for-in は同じで、`SubList` の for-in は 2 回から 1 回に、`forEach` は 1 回のままです。
- **検出には `IntList` の `modCount` を使います。** JMH(arm64)では、`modCount` なしとの差は n = 1000 で約 10% でした(`ops/ms` で 757 と 837。`ArrayList` は 429)。`modCount++` を `add` の最後に置くことが重要で、先頭に置くと約 2 倍遅くなりました。これを受けて検出方式は変更しません。
- **確保:** 呼び出しごとに 1 オブジェクトを確保します。ホットループの中では、`IntList` の範囲引数つき API(`sort(from, to)` など)の方が確実です。
- **バンドル:** `subList` を使っていない提出に `IntSubList` が含まれないように、`IntList` のメンバーではなく拡張関数として定義します。

**対象は `List` のみです(4 型)。** `Deque` は範囲の反復とコピーで足り、配列には標準の `copyOfRange` や範囲引数つきの `sort` があります。

#### 5.3.2 `fold` と `sumOf`(Phase 1 の実装で確定)

- **`fold` は標準と同じ `fold<R>` にします。** `Int`、`Long`、`Double` の別々のオーバーロードにすると、`fold(0) { ... }` が曖昧でコンパイルエラーになります。ジェネリックでも、inline 展開後はコンパイラの最適化でボクシングが消えます。ただし**直接のループで書いたときだけ**で、`forEach` の上に書くとボクシングが残ります(Kotlin 2.2.10 で確認。`MainKt` の厳格検査で回帰を検出)。**ジェネリックな inline 関数は、全キー型と、結果型 `Int`、`Long`、`Double`、`Char`、`Boolean` の組み合わせを `e2e/usage` から呼びます。** 結果がコンパイラの最適化次第で決まるのは、型引数を持つものだけです。ラムダの型がプリミティブで決まる `forEach` などは、展開してもボクシングしません。今の該当は `fold` だけで、`intlist-all` と `lists-other-types` で全組み合わせを呼んでいます。
- **`sumOf` は `Long` を返すセレクタのみです。** `Int` と `Long` の両方を受けるには実験的機能(`@OverloadResolutionByLambdaReturnType`)が必要で、§1.4 に反します。`Int` のセレクタは `sumOf { it.toLong() }` と書きます。

#### 5.3.3 全型への展開で決めたこと

- **キーの型ごとに名前を分けます(`sortBy`、`sortByLong`、`sortByDouble`)。** 同じ `sortBy` でラムダの戻り値の型ごとにオーバーロードするには実験的機能(`@OverloadResolutionByLambdaReturnType`)が必要で、`sumOf` と同じく §1.4 に反します。`argsortBy` も同じです。
- **`mapToLong` などの型変換は、組ごとに別のファイルの拡張関数にします(`IntListMapToLong` など 12 ファイル)。** 同じ型への変換は `map` です。メンバーや 1 つのファイルにまとめると、1 つの変換のために変換先のすべての型が提出に入ります。
- **`Char` には `sum` を作りません。** `sumOf` は残します。
- **`Double` はすべての操作で全順序です(§4.4)。** テストの基準には標準の `minOrNull` を使えません。`NaN` があると `NaN` を返すからです。

### 5.4 Deque(`IntDeque`)

リングバッファ(容量は 2 の冪、`and` でマスク)で実装します。

`addFirst`、`addLast`、`removeFirst`、`removeLast`、`first`、`last`、`*OrDefault`、`*OrElse`、`get(i)`、`set(i, v)`(先頭からの添字)、`addAll*`、`contains`、`indexOf`、`rotate`、`forEach`、`forEachReversed`、`toIntArray`(巻き戻し済み配列を返す)。スタックやキューの用途にも使います。

#### 5.4.1 実装で決めたこと(2026-11-08)

テンプレートは `templates/KTypeDeque.kt` と、標準コレクションとの変換を分けた `templates/KTypeDequeConversions.kt` です。4 型(Int、Long、Double、Char)を生成します。

| 分類 | 操作 |
|---|---|
| 要素 | `[]`、`[] =`、`first`、`last`、`firstOrDefault`、`lastOrDefault`、`firstOrElse`、`lastOrElse`、`size`、`lastIndex`、`indices`、`capacity` |
| 追加 | `addFirst`、`addLast`、`add`(`addLast` と同じ。`ArrayDeque` に合わせる)、`+=`、`addAll(IntArray / IntArray の部分範囲 / IntDeque / IntProgression / Iterable<Int>)` |
| 削除 | `removeFirst`、`removeLast`、`removeAt`、`removeValue`、`-=`、`clear` |
| その他 | `contains`(`in`)、`indexOf`、`rotate`、`ensureCapacity`、`forEach`、`forEachReversed`、`for (x in d)`、`+`、`-`、`copy`、`toIntArray`、`toList`、`toMutableList`、`appendTo`、`joinToString`、`equals`、`hashCode`、`toString` |
| 生成 | `IntDeque(capacity)`、`intDequeOf(...)`、`IntArray.toIntDeque()`、`IntProgression.toIntDeque()`、`Iterable<Int>.toIntDeque()` |

- **容量は 2 の冪で、拡張は 2 倍です。** 添字を `and (capacity - 1)` で計算するためです。`nextCapacity` の拡張率(暫定 1.5 倍、「積んである作業」)の決定は `Deque` には及びません。容量の上限は 2^30 で、`IntDeque` なら 4 GiB なので、ジャッジのヒープ(約 922 MiB)では先にメモリが尽きます。コンストラクタの容量も 2 の冪に切り上げます(`IntDeque(3)` は容量 4)。`IntDeque(0)` は何も確保せず、最初の追加で既定の 8 になります。
- **`removeAt` は短い側を動かします。** 両端に近い位置の削除が速くなります。`rotate(d)` も、右に `d` 回と左に `size - d` 回の少ない方で、1 要素ずつ動かします(確保なし)。
- **構造変更の検出は `List` と同じです(§5.3.1)。** 追加と削除が `modCount` を進め、`forEach`、`forEachReversed`、for-in は変更を検出して例外にします。`set` と `rotate` は要素数を変えないので検出しません。何も変わらない `addAll`(0 個)と `-=`(一致なし)は `modCount` を進めません。
- **`equals` は容量と内部の配置によらず、先頭からの並びで比べます。** `hashCode` は `List.hashCode` と同じ式です。`Double` は全順序です(§4.4)。
- **持たないもの:** `subList`(§5.3.1)、`removeFirstOrDefault`(空かどうかは `isNotEmpty` で見る。必要になったら足す)、`any` や `count` などの高階関数、`sort`、`toIntList`(Deque のファイルが `IntList` を提出に引き込むため。`d.toIntArray().toIntList()` で書ける)。いずれもコンパイルエラーになるだけで、黙ってボクシングしたり誤答になったりはしません。
- **バンドル:** `IntDeque` をキューとして使うだけの提出には、`IntDeque.kt`、`IntDequeConversions.kt`、`Support.kt` の 3 ファイルが入ります。`IntList` は入りません。出力全体は 18,234 バイトです(コミット `0844bb7` で、`IntDeque()` を作って `addLast`、`isNotEmpty`、`removeFirst` だけを呼ぶ 7 行の `Main.kt` を `./bundle -p` した値。利用コードも含むので、`Main.kt` によって変わる)。

### 5.5 ヒープ

**単純ヒープ(`IntMinHeap`、`IntMaxHeap`)**

`add`、`first`、`removeFirst`、`firstOrDefault`、`removeFirstOrDefault`、`replaceFirst`(pop + push を 1 回の sift で)、`addAll`、配列から O(n) で構築、`drainSortedTo`、`forEachUnordered`。

**ペアヒープ(`LongIntMinHeap`)**

```kotlin
val pq = LongIntMinHeap()
pq.add(0L, s)
while (pq.isNotEmpty()) {
    val d = pq.firstKey(); val v = pq.firstValue(); pq.removeFirst()
    // ...
}
```

Pair を返せないので `firstKey()` / `firstValue()` に分けます。内部は並列配列の 4 分ヒープです。AtCoder のコードテストでは 2 分ヒープとの差を判別できず、切り替える根拠がないので 4 分のままにしました(`docs/judge-env.md` §7)。

- **`Max`:** `Min` と同じテンプレートから、比較の向きだけを変えて生成します。別々に書かないので、片方だけ直し忘れることがありません。
- **`Double` キー:** `sort` と同じ全順序です。`-0.0` は `0.0` より小さく、`NaN` は最大です。
- **`Obj` 値(`LongObjMinHeap<V>`):** 値は `Array<Any?>` に持ちます。`removeFirst` と `clear` で参照を消すので、取り除いた値は GC の対象になります。`null` も値として使えます。`equals` は同じキーの値を `==` で突き合わせるので、1 つのキーに n 個の値があると O(n^2) かかります。
- **`forEachUnordered` の中での変更:** `add`、`removeFirst`、`replaceFirst`、`clear` を行うと `ConcurrentModificationException` になります。§5.6 と同じく、黙って誤答になるより RE で気づける方を選びます。`replaceFirst` は要素数が変わらないので、要素数の比較では検出できず、`modCount` を使います。

**インデックス付きヒープ**: 添字集合 `[0, n)` に対する `set(i, key)`、`decreaseKey`、`increaseKey`、`remove(i)`、`contains(i)`、`keyOf(i)`、`firstIndex()`。

**比較関数付きヒープ**: `IntComparator` を受け取る別クラスにし、Min / Max の高速経路に影響を与えません。

### 5.6 HashSet / HashMap

**内部設計**

| 項目 | 方針 |
|---|---|
| 方式 | オープンアドレス法、線形探索、2 の冪容量 |
| 削除 | Backward shift。墓標を使わず、劣化や無限ループを防ぐ |
| 空きの判定 | キー 0(または型ごとの番兵)を別フィールドで管理。任意のキー値を格納可能 |
| ハッシュ | 乗算 + シフトの強いミックス関数とインスタンス別ソルト |
| ソルト | 固定の初期値 + カウンタで決定的に生成。再現性を保ちつつ、引数で上書き可能 |
| 負荷率 | 0.5〜0.75 を実測で決定 |
| 容量上限 | 2^30。閾値は Long で計算し、超過時は明示的な例外 |
| 浮動小数点キー | `toBits()` で比較(NaN は同一、0.0 と -0.0 は別) |

インスタンス別ソルトを入れる理由は、**線形探索の表をイテレーション順に別の表へコピーすると、ほぼ 2 乗の時間になる**既知の劣化を防ぐためです。AtCoder には開催中のハック制度がないため、乱数シードより再現性を優先します(ただし `after_contest` テストの存在を踏まえ、シードの差し替えは可能にします)。

**Map の操作**

| 分類 | 操作 |
|---|---|
| 基本 | `get`(不在時はゼロ値、§4.3)、`set`、`getOrDefault`、`getOrElse {}`、`getOrPut {}`、`contains`(`k in map`)、`containsKey`、`containsValue` |
| 更新 | `addTo(k, delta)`(数値 V)、`merge(k, v) {}`、`update(k) {}`、`putIfAbsent`、`replace`、`compute` 相当 |
| 削除 | `remove`、`removeOrDefault`、`removeIf { k, v -> }`、`retainIf {}` |
| 反復 | `for ((k, v) in map)`、`forEach { k, v -> }`、`forEachKey`、`forEachValue`、`keysToArray`、`valuesToArray` |
| カーソル | `findSlot`、`keyAt`、`valueAt`、`setValueAt`。二重探索を回避 |
| その他 | `putAll`、`ensureCapacity`、`trim`、`copy` |

**Set の操作**: `add`、`remove`、`contains`、`addAll`、`removeAll`、`retainAll`、`containsAll`、`forEach`、`toArray`、`toSortedArray`、`removeIf`。

**イテレーション中の変更**: `modCount` で検出して例外にします(コストは実測で確認)。AtCoder で黙って誤答になるより、RE で気づける方が有益と判断します。

### 5.7 TreeSet / TreeMap / TreeMultiset

**内部設計**

ノードは並列配列 + Int 添字で表現し、フリーリストで再利用します。部分木サイズを持たせ、順序統計を O(log n) で提供します。再帰は使いません。

候補は次の 2 つで、Phase 4 の初めに実測して決めます。

| 案 | 長所 | 短所 |
|---|---|---|
| A. 配列ベース AVL / 重み平衡木 | 実装が単純。削除も素直 | N が大きいとキャッシュミスが多い |
| B. カウント付き B+ 木 | キャッシュ効率がよく、2〜3 倍速い可能性(未検証) | 実装が複雑 |

判定基準: B 案が N = 2×10^5〜10^6 の混合操作で **1.5 倍以上速ければ** B を採用します。テンプレートで一度書けば全型に展開されるので、複雑さは一度しか負担しません。

**Set の操作**

| 分類 | 操作 |
|---|---|
| 基本 | `add`、`remove`、`contains`、`addAll`、`clear` |
| 端 | `first`、`last`、`pollFirst`、`pollLast`(と `*OrDefault`、`*OrElse`) |
| 近傍 | `floor`、`ceiling`、`lower`、`higher`(と `*OrDefault`、`*OrElse`、`has*`) |
| 順序統計 | `kth(i)`、`rank(x)`(x 未満の個数)、`countInRange(lo, hi)`、`removeKth(i)` |
| 反復 | `forEach`、`forEachDescending`、`forEachInRange(lo, hi, 境界の開閉)`、`iterator`、`toArray` |

**Map の追加操作**: `get`(不在時はゼロ値、§4.3)、`set`、`contains`、`containsKey`、`getOrDefault`、`getOrElse`、`getOrPut`、`addTo`、`merge`、`floorKey` など、`kthKey`、`kthValue`、カーソル API(`floorNode(k)` → `keyAt` / `valueAt`、`NIL` で不在を表現)。

**Multiset の追加操作**: `add(x, count)`、`count(x)`、`removeOne(x)`、`removeAll(x)`、`distinctSize`、重複込みの `kth` と `rank`。

**サブマップビューは作りません。** ビューは割り当てと複雑さを増やすわりに使用頻度が低いので、範囲反復と範囲カウントで代替します。

### 5.8 周辺機能

| 機能 | 内容 |
|---|---|
| `IntPair` | `@JvmInline value class(val packed: Long)`。`first`、`second`、順序保存パック(下位側の符号ビットを反転し、辞書式順序と Long の順序を一致させる) |
| `FixedBitSet` | 長さ固定のビット列(§5.8.1)。`java.util.BitSet` にはシフトがない |
| `FastSet` | 64 分木で `[0, N)` の整数集合を管理。`floor` / `ceiling` が実質定数時間 |
| `CsrGraph` | 辺を入れてから一括構築する隣接配列。`forEachNeighbor(v) { }`、`degree`、重み付き(Long、Int)、有向 / 無向 |
| 配列拡張 | `IntArray.lowerBound`、`upperBound`、`argsort`、`argsortBy`、`sortBy`、`sortWith`、`dedupe`、`compress`(座標圧縮)、`nextPermutation`、`prefixSum` |

#### 5.8.1 `FixedBitSet`

**名前:** `BitSet` にすると `java.util.BitSet` と衝突します。`import ac_primitive_collections.*` と `import java.util.*` を併用すると `BitSet` はコンパイルエラーになる一方、同じファイルに貼り付けた場合は自前のクラスに解決されてコンパイルが通ります(Kotlin 2.2.10 で確認)。貼り付けと `import` で同じコードが動かなくなるので、別の名前にします。長さが固定であることも名前で示します。

| 分類 | 操作 |
|---|---|
| 要素 | `b[i]`、`b[i] = v`、`i in b`、`set(i)`、`clear(i)`、`flip(i)`、`size` |
| 範囲 | `setRange`、`clearRange`、`flipRange`、`clear()`、`fill()` |
| シフト(新しいビット列を返す) | `b shl k`、`b shr k` |
| シフト(その場で変更) | `shlAssign(k)`、`shrAssign(k)` |
| ビット演算(新しいビット列を返す) | `a and b`、`a or b`、`a xor b`、`b.inv()` |
| ビット演算(その場で変更) | `andAssign`、`orAssign`、`xorAssign`、`invAssign` |
| 部分和 DP 向け | `orShiftLeft(k)`(`dp = dp or (dp shl k)` と同じ結果を、確保なしで計算) |
| 検索・集計 | `cardinality()`、`nextSetBit(i)`、`prevSetBit(i)`、`nextClearBit(i)`、`prevClearBit(i)`、`isEmpty()`、`intersects(b)` |
| 反復・変換 | `forEachSetBit {}`、`for (i in b)`、`toIntArray()`、`toString()` |

Kotlin には `<<` がないので、`shl` / `shr` は `Int` と同じ中置関数にします。`b shl k` は毎回確保するので、ループでは `shlAssign` や `orShiftLeft` を使います。

**動作の仕様**

- 長さ `n` は生成時に固定し、自動では伸びません。`b[i]` の `i` が範囲外なら例外です。
- 範囲外へシフトされたビットは捨てます。`k >= n` なら全ビット 0、`k < 0` は例外です。
- 長さが異なるビット列どうしの演算は例外です。

**テストで重点的に確認する点**(`BooleanArray` と `BigInteger` のモデルと差分テスト)

- シフト量: 0、1、63、64、65、64 の倍数(`Long` のシフトは 6 bit に丸められるので、語の境界で壊れやすい)、`n - 1`、`n`、`n + 1`、`Int.MAX_VALUE`
- 長さ: 1、63、64、65、64 の倍数
- 末尾の余りビット: `inv`、`fill`、`shl` の後も `n` 以上のビットが 0 で、`cardinality` と `equals` が正しいこと
- `orShiftLeft` と `a.orAssign(a)` のような、同じ配列を読み書きする操作の重なり

#### 5.8.2 名前の衝突検査

`java.util`、`kotlin`、`kotlin.collections`、ジャッジ導入済みライブラリの公開クラスと、本ライブラリの公開名が衝突しないことを CI で検査します。

---

## 6. ソート戦略

原則は **「仮説ではなく、JDK 23 の実測で採否を決める」** です。

| 対象 | 既定の実装 | 検討する代替 | 採用条件 |
|---|---|---|---|
| Int 自然順 | `Arrays.sort` | なし | **決定**: 自作しない。5×10^6 要素で最小 37 ms |
| Long / Double 自然順 | `Arrays.sort` | なし | **決定**: 自作しない。Long は 5×10^6 要素で最小 74 ms。8 bit 基数ソートは 167 ms で、採用条件(1.3 倍以上速い)を満たさない |
| Char | `Arrays.sort`(JDK が計数ソートを使用) | なし | 自作不要 |
| 降順 | 昇順 + 反転 | なし | 符号反転は使わない |
| `argsort`、`sortBy { intKey }` | `(key.toLong() shl 32) or i` を Long ソート | | 比較関数不要で安定。実機で Long の `Arrays.sort` が速いので、Int キーで最速の見込み |
| `sortByLong {}`、Long の `argsort` | 8 bit の LSD 基数ソートで添字を並べる(`KeySort.kt`) | | 64 bit に収まらないため別実装。比較関数が不要で安定。**未計測**で、採否は下の「ソートの採否」で決める |
| `sortByDouble {}`、Double の `argsort` | ビット変換で全順序の Long に変換し、`sortByLong` と同じ処理 | | `-0.0` と `NaN` の順序を `Double.compare` と一致させる |
| `sortWith(comparator)` | `IntComparator` + 反復版の pdqsort 系 / マージソート | | inline は再帰と巨大メソッドの制約があるため `fun interface` を使用 |
| 安定ソート | 反復版マージソート | | 作業領域を再利用 |

**注意点**

- 上記の決定は、コードテストでの 1 回の計測に基づきます。コードテストと本番のジャッジが同じ構成かは未確認です。AVX-512 のないマシンでは結果が変わる可能性がありますが、`Arrays.sort` は性能が落ちても正しく動くので、このまま採用します。次回の言語アップデートでは計測し直します。
- `Arrays.parallelSort` は使いません。実測でプロセッサは 1 個です。
- **JDK の `DualPivotQuicksort.java` は移植しません。** GPL + Classpath Exception のコードです(§10)。
- 比較関数ソートの最悪計算量を保証します(深さ制限つきでヒープソートへフォールバック、スタックは固定長)。
- 比較関数が全順序に反していても、配列外アクセスや無限ループを起こさない実装にします。

---

## 7. テスト計画

### 7.1 品質ゲート

次のゲートをすべて満たすことを完了条件とします。

| ゲート | 基準 |
|---|---|
| 分岐カバレッジ | 全生成クラスで 100%。除外は個別に理由を記録 |
| 変異テスト(PIT) | 検出されなかった変異が、すべて `docs/mutation-review.md` の「確認済みの生存変異」の表にあり、生成クラスごとの件数が表の上限以下(`pitestBaseline`)。新しい生存変異は、テストを足すか、等価だと確認して表に理由と一緒に足す。検出率は参考値として記録する(2026-11-08 に変更。経緯は `docs/mutation-gate.md`) |
| 差分テスト | §7.2 を全クラス・全シードで通過 |
| ボクシング検査 | 例外リスト以外で検出ゼロ |
| 確保量テスト | §7.3 で定義するホットパスで 0 バイト |
| E2E | 実ジャッジ構成で全サンプル通過 |
| 実問題 | AtCoder 過去問 30 問以上で AC(§7.5) |

**残るリスク**: JIT のバグ、ジャッジの CPU 差異、テスト自体の誤り。参照モデルが `java.util` なので、同じ仕様解釈の誤りは検出できません。API ごとの手書きテストで補います。

### 7.2 テストの種類

| 種類 | 内容 |
|---|---|
| **モデル差分テスト** | ランダムな操作列を `java.util`(`ArrayList`、`ArrayDeque`、`HashMap`、`TreeMap`、`PriorityQueue`)と同時に実行し、毎操作で結果を、定期的に全内容を比較。テスト側のボクシングは許容 |
| **操作分布** | 追加偏重、削除偏重、小さな値域(衝突や重複が多い)、単調増加、空付近の往復 |
| **境界値** | 全型で `MIN`、`MAX`、`0`、`-1`、`1`。浮動小数点は `NaN`、`±0.0`、`±Infinity`、`MIN_VALUE`。空、要素 1 個、容量ちょうど、拡張直前直後 |
| **網羅テスト** | 小さいサイズでの全順列(ソート)、全操作列(n ≤ 4〜5 程度) |
| **不変条件検査** | テスト専用の `checkInvariants()`。木の平衡と部分木サイズ、ヒープ条件、ハッシュ探索列の連続性、リングバッファの範囲、カウンタの一致 |
| **ソート** | ランダム、昇順、降順、全同値、のこぎり波、organ pipe、クイックソートキラー列、基数ソートの境界。結果は `Arrays.sort` と一致を確認し、安定性も検証 |
| **ハッシュ耐性** | 連番、等差、上位ビットだけ異なるキー、2^k の倍数、同じ順序での表コピーで線形時間になること |
| **例外仕様** | 空に対する操作、範囲外の添字、不正な範囲、イテレーション中の変更 |
| **`subList`** | `ArrayList.subList` との差分テスト(全操作、入れ子、`from == to`、全範囲)。親への反映、親の構造変更後の例外、`clear()` 後の親と親の `modCount` の整合 |
| **演算子** | §4.8 の全記法を、対応する標準コレクションと差分テスト。`a + x` が元を変更しないこと、`a += x` がその場で変更されること、自己参照(`a += a`) |
| **相互運用** | 標準コレクションとの往復変換で内容が一致すること(空、`MIN` / `MAX`、`NaN`、`±0.0` を含む)、Tree 系の変換で順序が保たれること、変換後の変更が元に影響しないこと |
| **不在時の `map[k]`** | 全組み合わせでゼロ値を返すこと、読み取りでサイズが変わらないこと、値 0 のキーが `k in map` では存在扱いになること |
| **自己参照** | `addAll(self)`、`putAll(self)`、`retainAll(self)` |
| **オーバーフロー** | `nextCapacity` などの純粋関数を境界値で単体テスト。実際に 2^31 付近まで拡張するテストは行わない(`ByteList` を作らず、`IntList` では 8 GiB 必要で、ジャッジのヒープ約 922 MiB でも届かないため) |
| **意味論** | `equals` / `hashCode` の整合、`copy` の独立性、`clear` 後の再利用、`toString` |

シードは失敗時に出力し、`-Dseed=...` で再現できるようにします。失敗した操作列は自動で短縮して(ddmin)、回帰テストとして追加します。

**実行量**: PR では各クラス 10^4 操作 × 数シードで 10 分以内を目標にします。夜間は 10^6〜10^7 操作に増やします。

**テストの生成**: テストもテンプレートから生成し、全型に適用します。テンプレートのバグが本体とテストの両方に入らないように、期待値は実装から導かず、必ず参照モデルか手書き値から得ます。

### 7.3 ボクシング検査(最重要ゲート)

ASM ですべてのクラスのバイトコードを走査し、次を検出したら失敗にします。

- `java/lang/{Integer,Long,Double,Float,Short,Byte,Character,Boolean}.valueOf`
- 上記クラスの `intValue` などのアンボクシング呼び出し
- `kotlin/jvm/internal/Boxing`
- 公開シグネチャ上の `Ljava/lang/Integer;` など(`Int?` を返す API の検出)
- プリミティブ型クラス内の `kotlin/Function*` の使用、`kotlin/Pair` の使用

**ライブラリ単体では不十分です。** inline 関数は利用側に展開されるので、代表的な利用コードを `kotlinc 2.2.10` でコンパイルして、その `Main.jar` のうち `MainKt` にも同じ検査をかけます。`for (x in list)` の展開もここで確認します。

**検査の例外**

| 対象 | 扱い |
|---|---|
| `@BoxingApi` を付けた変換(`toList()` など) | 検査対象外 |
| ラムダを受け取る public `inline fun` の、ライブラリ側に残る本体(§4.2) | 検査対象外。Kotlin メタデータで inline であることを確認し、非 inline の関数がすり抜けないようにする |
| 利用側の `MainKt` | **例外なし**。展開後のコードにボクシングがあれば失敗 |

加えて次の 2 つを検査します。

- **確保量テスト**: `com.sun.management.ThreadMXBean#getThreadAllocatedBytes` で、ウォームアップ後の確保量を確認します。対象は次のとおりです。
  - **0 バイトを要求:** `get`、`set`、容量内の `add`、`contains`、ヒープの操作、Map の `get` / `set`(拡張なし)、`forEach {}`、`for (i in 0 until list.size)`
  - **ウォームアップ後に 0 バイトを要求(初回の確保は許容):** `for (x in list)`、`for (i in list.indices)`、`for ((k, v) in m)`
  - **確保を許容:** `a + x`、`map {}`、`subList` など、新しいオブジェクトを返す操作
- **メソッドサイズ**: 全メソッドが 8000 バイトコード未満であること、代表的な利用コードの展開後のサイズを監視します。

### 7.4 テスト基盤

JUnit 5 + kotlin-test を使います。操作列の実行器は自作して依存を増やしません。カバレッジは Kover、変異テストは PIT(Gradle プラグイン)、バイトコード検査は ASM です。いずれもテスト時のみの依存で、配布物には含みません。

### 7.5 E2E と実問題

| 段階 | 内容 |
|---|---|
| ジャッジ再現環境 | 公式 TOML の install スクリプトと同じ構成の Linux x64 Docker イメージを作成(Kotlin 2.2.10、JDK 23.0.2) |
| 貼り付け方式(現在の提出形態) | バンドラの出力を `Main.kt` にして、§2.1 と同じコマンドでコンパイル・実行。**テストスイートの代表ケースもこの形で実行** |
| jar 方式(将来の提出形態) | §2.1 のコマンドの `-cp` と `kotlin.sh` に本 jar を追加してコンパイル・実行。**同じ `Main.kt` で**貼り付け方式と出力が一致することを確認 |
| インストールのリハーサル | ジャッジ再現環境に、提案予定のスクリプトで jar を導入できることを確認 |
| 代表問題 | Dijkstra、カウンタ、順序統計、グリッド BFS、座標圧縮など、AtCoder 過去問 30 問以上を実際に AC し、`java.util` 版との時間とメモリを比較して記録 |
| 実機確認 | AtCoder のコードテストで動作と速度を確認 |

### 7.6 性能・サイズの基準

計測環境ごとに、判断に使える範囲を分けます。

| 環境 | 用途 | 判断に使えない理由や制約 |
|---|---|---|
| **AtCoder コードテスト** | **`java.util` との比較と設計判断**。同じファイルに両方の実装を書いて 1 回実行で比較 | JMH や外部 jar は使えない |
| Linux x64 の CI | JMH(`-prof gc`)による回帰検知。fastutil、HPPC との比較は参考値 | CPU や負荷がジャッジと異なる |
| ジャッジ再現環境(Docker) | コンパイルと動作の互換性の確認のみ | arm64 ではエミュレーションになる |
| 開発機(arm64) | テストと開発中の目安のみ | SIMD の条件が異なり、Phase 0 の例のように結果が逆転する |

**1 回実行の時間を重視します。** AtCoder の実行は数百 ms〜数秒と短く、プロセッサが 1 個なので、JIT コンパイルと本体が同じ CPU を取り合います。実測では、200,000 要素のソートの 1 回目は最小値の約 10 倍かかりました(51 ms と 5 ms)。定常状態の JMH だけで判断せず、1 回実行の総時間(`-bm ss` 相当と実プロセス起動)を `-XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:MaxHeapSize=1000341504` で計測します。バイトコードが小さいほど JIT の負担も減るので、inline の展開量もこの観点で評価します。

| 指標 | 目標(実測後に確定) |
|---|---|
| ホットパス | コードテストで `java.util` に勝つ。fastutil と HPPC との差は参考として記録 |
| 起動オーバーヘッド | 未使用クラスのロードなし。Hello World との差がノイズ以内 |
| **バンドル後のソースサイズ** | 典型的な提出(数クラス利用)で上限の 1/4 以下。最大級の組み合わせでも上限内 |
| **バンドルによるコンパイル時間の増加** | 上限は未確認(§15)。大きな提出が現実になったときに実測して、余裕を確認する |
| jar サイズ | 1.5 MB 以下(HPPC 程度)。インストール提案の材料として毎リリース記録 |
| jar 追加によるコンパイル時間の増加 | +1 秒以内。全 Kotlin 提出に影響するので、ジャッジ導入時の重要な指標 |
| メモリ | 同等の `java.util` 比で 1/3 以下 |

**性能の計測は貼り付け形式でも行います。** 提出時はコンパイルオプションを指定できないので、`-Xno-param-assertions` などのライブラリ向け最適化は当てにできません。判断には、バンドル後のコードを §2.1 のコマンドでコンパイルしたものの結果を使います。

**配列の拡張はヒープ上限よりかなり手前で失敗します**(Serial GC の設定を再現した実験、`docs/judge-env.md`)。

| 拡張率 | `IntList` が拡張で到達できる要素数(目安) |
|---|---|
| 2 倍 | 約 7100 万 |
| 1.5 倍 | 約 8700 万 |
| 拡張なし(事前確保) | 約 1.67 億(1 個の配列の上限 約 637 MiB) |

- 通常の制約(N ≤ 10^7 程度)ではどの拡張率でも届くので、**拡張率は到達要素数ではなく、1 回実行の時間と平均メモリで決めます**(1.5 倍と 2 倍を比較)。
- 要素数が分かっているときに事前確保できるように、`IntList(capacity)` と `ensureCapacity` を用意します。
- 使えるヒープは約 922 MiB で、問題のメモリ制限が大きくても増えません(物理メモリが同じ場合)。
- ハッシュ表は 2 の冪容量で、新旧の配列が同時に必要です。到達できる要素数を Phase 3 で同じ設定で測り、KDoc に書きます。

---

## 8. 配布とバンドラ

### 8.1 配布の 3 形態

| 形態 | 用途 | 状態 |
|---|---|---|
| **バンドラ**(`Main.kt` + ライブラリ → 1 ファイル) | **ジャッジ導入までの提出手段** | 必須 |
| 生成済みソース(`generated/` にコミット) | 手動コピー、ルール上の根拠(§9) | 必須 |
| **jar** | 手元の開発(IDE 補完、テスト)。**ジャッジ導入後の提出手段** | 必須。リリースごとに公開 |

### 8.2 バンドラの要件

- `Main.kt` で使っているクラスを検出し、依存を含めて必要なものだけを結合します(クラス単位)。
- 1 ファイルには 1 パッケージしか書けないため、`package` と本ライブラリの `import` を除去します。
- **参照 URL を自動で付与します。** ルール上はリポジトリへのリンクで足りますが、内容を特定できるようにタグに固定したリンクを使います(§9)。
- **名前の短縮や難読化はしません。** ルールで問われるのはレビューを困難にする意図の有無で、短縮そのものが一律に禁止されているわけではありませんが、疑われる処理は避けます。空行とコメントの除去だけを任意で行います。
- 出力サイズが上限を超えそうな場合は警告します。ソース長の上限は未確認です(§15)。
- 手元では `import` で開発し、バンドラの出力を提出します。両方の動作の一致を E2E で確認します。
- **依存は余分に含める側に倒します。** 取りこぼすとコンテスト中に CE になりますが、余分なファイルはサイズが増えるだけだからです。含めたファイルの識別子は、メンバーの名前も含めてすべて依存の候補にします(メンバーが同名の拡張関数に処理を任せることがあるため。除外していたときと比べて、`List` を使う提出が約 1.3 KB 増える)。そのため、`IntList.mapToDouble` と `DoubleList.argsort()` だけの提出にも、`IntListSort.kt` が入ります。
- ライブラリの型への拡張関数は、その型のファイルが含まれているときだけ、名前の一致を依存と見なします。`sortBy` などは型ごとにあるので、名前だけで解決すると、1 つの型を使う提出に 4 型すべてが入るためです。ある型の値を持つコードは、どこかでその型の名前に到達するので、必要なものは漏れません。
- コンテスト用は `origin` にある最新のタグ(`v` と数字だけのもの。比較は数値で行う)からバンドルします。今のチェックアウトには依存しません。手元の実行は作業ツリーのライブラリを使うので、提出に含むファイルが手元と異なるときと、タグにない名前を使っていそうなときは警告します。後者は利用者のローカル変数と区別できないので、エラーにはしません。
- 過去問の提出では、`--allow-unpublished` で作業ツリーからバンドルできます(§9)。
- 生成するソースは **Kotlin 2.2 の言語機能の範囲で書きます。** ジャッジの `kotlinc 2.2.10` でコンパイルされるためです。
- 実行時に生成 AI を使いません。ローカルで完結するので、開催中も使用できます。

### 8.3 貼り付けと `import` の差

Kotlin 2.2.10 の実験で、次の差を確認しました。いずれもコンパイルエラーになるケースで、黙って動作が変わるケースは今のところ確認していません。

| ケース | `import` | 貼り付け | 対策 |
|---|---|---|---|
| `import java.util.*` と併用して、同名のクラスを使う | エラー | 成功 | 公開名の衝突検査(§5.8.2) |
| 利用者が `IntArray.lowerBound` を定義する | 成功(利用者側が優先) | エラー | バンドラが衝突を検出して、分かりやすく報告 |
| 利用者が `class IntPair` を定義する | 成功 | エラー | 同上 |
| 利用者がライブラリの内部名(`checkRange`、`DEFAULT_CAPACITY` など)をトップレベルに定義する | 成功 | 同じシグネチャならエラー | バンドラは名前だけで判定するので、引数が違って貼り付けても動く場合も、改名を求めるエラーにする。誤答にはならず、数秒で直せる。貼り付けは暫定手段で、内部名にすべて接頭辞を付ける読みにくさは導入後も残るので、根本対策は行わない(2026-11-08 に決定) |
| ライブラリの `internal` を呼ぶ | エラー | 成功 | バンドラがトップレベルの名前を検出して**警告**する。ローカル変数と区別できず、貼り付けならコンパイルできるので、本番で提出を止めないようエラーにはしない。**メンバー(`list.elements` など)は検出できない**(コンパイルすると提出が遅くなるため)。ジャッジ導入後に初めて気づく可能性があるが、使わないことで避ける(2026-11-08 に所有者が確認) |

結合処理にも、次の 2 つの制約があります。

| 問題 | 対策 |
|---|---|
| 別ファイルにある同名の `private` 関数は、1 ファイルに結合するとエラーになる | ライブラリでトップレベルの `private` 関数の名前を重複させない。CI で検査 |
| `java.util.Random` と `kotlin.random.Random` を両方 `import` するとエラーになる | ライブラリ内では、衝突しうる名前を完全修飾名で書く。バンドラが `import` を統合する |

---

## 9. 生成 AI ルールへの対応

本ライブラリの実装に生成 AI(Claude Code)を使う場合、次を満たす必要があります。

| # | 対応 | 担保方法 |
|---|---|---|
| 1 | リポジトリを**公開**する | Phase 1 から公開リポジトリで開発 |
| 2 | **生成後のコードも公開**する | 生成済みソースを `generated/` にコミット。テンプレートとの一致は CI で検査。提出コードと同じ内容が事前に公開されていたことを、URL だけで確認できる |
| 3 | コンテスト前にタグを付けて push する | 利用するのは push 済みのタグのみ。コンテスト用のバンドルは、`origin` が報告する最新タグのコミットからソースを読むので、作業ツリーの未公開の変更は混ざらない。ルールはコンテスト開催中のみに適用されるので、過去問の提出では `--allow-unpublished` で解除できる |
| 4 | 提出時に参照 URL を添付 | バンドラが自動で付与 |
| 5 | README に利用条件を明記 | 「他の利用者も URL の添付が必要」と明記。他人が知らずにコピーしても違反になるため、ソースのヘッダにも記載 |
| 6 | 開催中は生成 AI でライブラリを修正しない | 運用ルールとして README に明記 |

**ルールの改定を継続的に確認します。** 最新版は 2026-10-14 版で、頻繁に更新されています。解釈に不安があれば、AtCoder に確認します。

ジャッジに導入された後は `import` のみでコードをコピーしないので、URL 添付の扱いが変わる可能性があります。導入時に確認します。なお、安全のため URL は付け続ける運用とします。

---

## 10. ライセンス

### 10.1 決定: CC0-1.0

CC0-1.0 を採用します。貼り付け提出に表記の義務がなく、ac-library-java と同じライセンスなのでジャッジ導入の前例もあります。

なお、CC0 でライセンス表記の義務はなくなりますが、**生成 AI ルールの参照 URL の添付は別の義務**なので、引き続き必要です(§9)。

### 10.2 必要な表記

| 場所 | 内容 |
|---|---|
| `LICENSE` | 全文 |
| 全ソース、テンプレート | `SPDX-License-Identifier` ヘッダ |
| **生成コード** | 生成器が同じヘッダを出力 |
| jar | `META-INF/LICENSE`、`META-INF/THIRD_PARTY_NOTICES` |
| バンドラ出力 | SPDX 行と参照 URL |
| `README` | ライセンス名 |
| インストール依頼 TOML | `license` フィールド(既存エントリの形式に合わせる) |
| CI | `reuse lint` でヘッダ漏れを検出(導入時に `LICENSES/` の追加と `gradlew` などの扱いの調整が必要な見込み) |

### 10.3 コードの出所の管理

| 参考元 | ライセンス | 扱い |
|---|---|---|
| JDK(`DualPivotQuicksort` など) | GPLv2 + CE | **コードを移植しない** |
| fastutil、Eclipse Collections | Apache-2.0 など | 公開された手法のみ参考にし、コードはコピーしない |
| pdqsort | zlib | 論文と仕様から実装。コードを移植する場合は NOTICE に記載 |
| 実行時依存 | なし | Kotlin stdlib は `-include-runtime` で供給されるので同梱しない |

テスト用の依存(JUnit、ASM、PIT など)は配布物に含みません。依存ライセンスの一覧は生成して保管します。

---

## 11. 実装フェーズ

順序は技術的リスクの高い順です。各フェーズの完了条件を満たしてから次に進みます。

### Phase 0: 環境固定と実測

**ビルド設定**

| 項目 | 設定 | 備考 |
|---|---|---|
| Kotlin | Gradle プラグイン 2.2.10 | ジャッジとコンパイラごと揃える |
| JDK | `jvmToolchain(23)` | クラスファイルは major 67(Java 23) |
| Gradle | 8.14.3 | Kotlin Gradle プラグイン 2.2.10 の対応範囲(7.6.3〜8.14)内。Gradle 8.14 は Java 24 までで動くので、Gradle の実行にも JDK 23 を使う。CI でも JDK 23 を明示する |
| group | `ac_primitive_collections` | §13 |

提出コードはジャッジの `kotlinc 2.2.10` でコンパイルされるので、**手元のテストも同じコンパイラで行います。** `JudgeToolchainTest` で Kotlin stdlib と JDK のバージョンを検証します。コンパイラの版は Gradle プラグインの指定で固定します。手元の JDK はパッチ版まで一致するとは限らないので(23.0.2 との完全一致はジャッジ再現環境で担保)、テストでは 23 系であることのみ確認します。

**Phase 0 の進捗**

| 項目 | 状態 |
|---|---|
| ビルド設定、`git init`、`LICENSE`(CC0-1.0) | 完了 |
| バイトコード実験の 2.2.10 での再確認(§4.6、§4.8) | 完了 |
| 計測コード `e2e/judge-probe/Main.kt` | 完了。`kotlinc 2.2.10` でコンパイルと実行を確認 |
| `e2e/judge/Dockerfile` | 完了。ビルドと、ジャッジのコマンドでのコンパイルと実行を確認。ベースイメージは推定 |
| AtCoder コードテストでの実測、`docs/judge-env.md` | 完了 |
| ソース長とコンパイル時間の上限の確認 | Phase 1 に移動 |
| Kotlin と JDK のバージョンの 1 か所への集約 | 完了(`gradle.properties`) |
| Gradle の実行 JDK の固定 | 完了(`gradle/gradle-daemon-jvm.properties`)。手元の `JAVA_HOME` が JDK 25 でも動く |

開発機は arm64 なので、Docker は x64 エミュレーションになります。互換性の確認には使えますが、**性能の判断には使えません**。性能の判断は AtCoder コードテストの結果を基準にします(§7.6)。

この方針が必要な実例があります。5×10^6 要素の Long のソートは、ローカル(arm64)では `Arrays.sort` 313 ms に対して基数ソート 66 ms でしたが、コードテストでは `Arrays.sort` 74 ms に対して基数ソート 167 ms と**逆転しました**。

Kotlin と JDK のバージョンは 1 か所の設定にまとめ、次回の言語アップデート時に差し替えられるようにします。将来のバージョンへの事前対応はしません。

**完了条件**: ジャッジ再現の Docker イメージのビルドと動作確認、実測値の記録(`docs/judge-env.md`)。**達成済み。**

### Phase 1: 縦切り(`IntList` のみで全パイプライン)

生成器、テンプレート、差分テスト、ボクシング検査、確保量テスト、JMH、バンドラ、リリース手順、E2E、ライセンス検査、CI を、**1 クラスで全部通します**。ソース長とコンパイル時間の上限もここで実際に提出して確認します。

**リリース手順**(コンテストで使えるようにするため、ここで定義します)

1. 生成して `generated/` をコミット(CI でテンプレートとの一致を検査)
2. タグを付けて push
3. バンドラがタグの公開を確認して、タグに固定した URL を付与
4. CI がタグで全ゲートと E2E を通したあと、`release` ジョブ(`install/release.sh`)が jar を作り、GitHub のリリースに添付する。§1.4 の「リリースごとに公開」と、インストールスクリプトの URL の根拠。リリースのノートに、jar のサイズと SHA-256 が載る
   - バージョンはタグから取られ、jar のマニフェストの `Implementation-Version` に入る。タグ以外で作った jar は `0.0.0-SNAPSHOT`。一致しなければジョブが失敗し、公開しない
   - `v` と数字だけではないタグ(`v1.0-rc1` など)では、何も公開しない
   - 手元での確認は `./install/release.sh vX.Y.Z --dry-run`(タグのチェックアウトで実行)
5. 下の表に、jar のサイズと、`./e2e/run-e2e.sh` の jar 方式のコンパイル時間を記録する。コンパイル時間は Docker のエミュレーションの参考値で、ジャッジの値ではない(jar をジャッジに置けないため、導入前は実測できない)

**タグの push は、それだけで GitHub のリリースの公開まで進みます**(2026-11-08 に所有者が決定)。push の許可が、公開の許可を兼ねます。

| タグ | jar サイズ | jar 方式のコンパイル時間(Docker、参考) | 備考 |
|---|---|---|---|
| `v0.2.0` | 記録なし | 記録なし | 手順 4、5 を定める前のタグ。GitHub のリリースもない |
| `v0.3.0` | 232,451 バイト | 12.3〜20.6 秒(6 サンプル。最大は `lists-other-types`。貼り付けは 16.5〜52.2 秒) | `release` ジョブの初回。公開された jar の SHA-256 とマニフェストのバージョンを手元で照合済み |

**生成器の設計**

Gradle の `build-logic` に Kotlin で実装し、外部のテンプレートエンジンや KSP に依存しません。

```kotlin
// templates/KTypeList.kt — そのままコンパイル可能な正しい Kotlin
typealias KType = Int
typealias KTypeArray = IntArray

class KTypeList { /* KType を使って実装 */ }
```

- **テンプレート自体が正しい Kotlin** です。IDE の補完やリファクタリングが効きます。Int 版としてそのままテストでき、トークン置換で他の型を生成します。
- 条件分岐は `//#if KEY_IS_FLOATING ... //#endif` のマーカーで書きます。
- 型の差(比較、等値、ハッシュ、ゼロ値、番兵)は型定義テーブルにまとめます。
- 一部の型だけの特殊実装は手書きファイルで上書きできます。
- 出力は決定的にして、CI で再生成の一致を確認します。
- 生成物は `generated/` にコミットし、ビルドもそこから行います(§9)。

**完了条件**: `IntList` がすべてのゲートを通過し、バンドラ出力で AtCoder に実際に提出して AC できること。

**Phase 1 の進捗**

| 項目 | 状態 |
|---|---|
| 生成器、テンプレート、`generated/`、一致検査 | 完了(Int のみ生成。他の型の展開は Phase 2 で初めてコンパイルされる) |
| `IntList`、`IntSubList`、変換、比較関数ソート、argsort | 完了 |
| 差分テスト、ddmin、網羅テスト、手書きテスト、例外仕様 | 完了 |
| ボクシング検査(ライブラリと `MainKt`)、メソッドサイズ、確保量 | 完了。検査器自体もテスト済み |
| 分岐カバレッジ | 100% |
| 変異テスト | 96%。生存変異の確認は `docs/mutation-review.md` |
| 禁止パターン、`private` の重複、名前の衝突、`reuse lint` | 完了 |
| バンドラ、公開確認、参照 URL の付与 | 完了 |
| E2E(貼り付けと jar、Docker)、インストールのリハーサル | 完了 |
| JMH | 完了(arm64 の値のみ。§7.6 のとおり判断には使わない) |
| CI | 完了(タグと PR #1 で通過) |
| タグの作成と push | 完了(`v0.1.0`) |
| AtCoder への実提出と AC | 完了。ABC 231 C を貼り付け形式で提出して AC、509 ms(2026-11-06、未公開状態のコードで、終了済みコンテストへの提出) |
| コードテストでの `java.util` との比較 | 完了(`docs/judge-env.md` §6) |
| 拡張率、ソース長とコンパイル時間の上限 | 拡張率は **未実施**(本人の操作が必要)。上限の調査は後回しにした(§15)。プローブは `e2e/experiments/` |

**Phase 2 に残したもの**

どちらも全型の `List` で対応しました。

- `mapToLong` などの型変換、`sortByLong`、`sortByDouble`
- `argsort`、`sortBy` の実装が Int のキーを前提にしていた問題(`KeySort.kt` の基数ソートを追加)

### Phase 2: 線形構造とソート

全型の `List`、`Deque`、単純ヒープ、ペアヒープ、インデックス付きヒープ、配列拡張。ソートの実測(§6)と採否の決定。

**完了条件**: Dijkstra と argsort の代表問題が AC。`java.util` 比の性能を記録。

**Phase 2 の進捗**

| 項目 | 状態 |
|---|---|
| `LongIntMinHeap`(テンプレート `KTypeVTypeMinHeap`、4 分ヒープ) | 完了。全ゲートを通過(分岐カバレッジ 100%、変異テスト 96%)、E2E 通過 |
| Dijkstra の代表問題 | 完了。ABC 340 D で AC。`modCount` を入れた後の今のコードで 269 ms(入れる前は 239 ms、`docs/judge-env.md` §7) |
| `PriorityQueue` との比較 | 完了。Dijkstra 部分が 2.3 倍速い。`modCount` を入れた後も処理時間に悪化は見られない(`docs/judge-env.md` §7) |
| タグの作成 | 完了。`v0.2.0` でペアヒープを、`v0.3.0`(2026-11-08)で全型の `List` をコンテストで使える。`v0.3.0` から、jar を GitHub のリリースで公開している |
| 他の型の組み合わせと `Max` のペアヒープ | 完了。§5.1 の 18 クラスを 1 つのテンプレートから生成し、全ゲートを通過(分岐カバレッジ 100%、変異テスト 91%)、E2E 通過。AtCoder への提出は判断が変わらないので行っていない |
| 全型の `List` | 完了。Int、Long、Double、Char の `List`、`SubList`、変換、ソートと、型変換 12 ファイルを生成し、全ゲートを通過(分岐カバレッジ 100%、変異テスト 93%)、E2E 通過。決めたことは §5.3.3。テンプレートは Int 版として `check` でコンパイルされる(§11)。実際の問題での提出は、判断が変わらないので行っていない |
| 変異テストのゲートの見直し | 完了(2026-11-08)。検出率の閾値をやめ、確認済みの生存変異との差分をゲートにした(§7.1、`docs/mutation-gate.md`)。x64 の CI の nightly でも通過した(`b1c07a3`) |
| 全型の `Deque` | 完了(2026-11-08)。Int、Long、Double、Char の `Deque` と変換を生成し、全ゲートを通過(分岐カバレッジ 100%、変異テストは確認済みの生存変異の表に収まる)。決めたことは §5.4.1。E2E の `e2e/usage/deque-all` に、BFS と 0-1 BFS を入れた。AtCoder への提出は、判断が変わらないので行っていない |
| 単純ヒープ、インデックス付きヒープ、配列拡張 | **未実施** |
| ソートの実測と採否(§6)、argsort の代表問題 | **未実施** |

**積んである作業**

どれも未着手です。順序と着手の時期は所有者が決めます。AtCoder での手作業が必要なものは、着手するときに手順を具体的に示して依頼します。

| 作業 | 内容 | 手作業 |
|---|---|---|
| `nextCapacity` の拡張率の決定 | 暫定で 1.5 倍のまま | コードテスト(`e2e/experiments/GrowthRate.kt`) |
| 単純ヒープ、インデックス付きヒープ、配列拡張 | §5.5、§5.8 | なし |
| ソートの採否と argsort の代表問題 | §6。Phase 2 の完了条件。`Long`、`Double` の `argsort` と `sortByLong` などの既定である基数ソートも測る。基数ソートは測らずに `v0.3.0` に入れた(2026-11-08 に所有者が決定。`v0.2.0` には `Long`、`Double` の `argsort` がなく、悪くなるものがないため) | 提出 |
| 標準と同じ書き方の不足 | `sumOf` は `Long` のセレクタだけなので、`doubles.sumOf { it * 2.0 }` はコンパイルエラーになる(`sumOfDouble` なら `sortByDouble` と揃う)。`String` から `CharList` を直接作れず、`s.toCharArray().toCharList()` と 2 回コピーする。どちらもコンパイルエラーか小さな無駄で、誤答や黙ったボクシングにはならないので、必要になったら足す | なし |
| メモリの目標の確認 | §15。JVM の基礎分を差し引いて、`java.util` 比 1/3 以下を満たすかを確かめる | コードテスト |
| `Deque` の生存変異の表の x64 での確認 | 変異テストのゲートは、`Deque` を入れる前のコード(`b1c07a3`)では x64 の CI でも通った(`docs/mutation-gate.md` §5)。`Deque` の行を足した表は、まだ x64 で動いていない。PR #11 をマージした後の最初の nightly か `gh workflow run ci.yml --ref main` で通ることを確かめる。失敗したら、表にない変異が、手元と CPU が違うことによる `TIMED_OUT` と `SURVIVED` の揺れかどうかを見る | なし |
| キーソートのファイル分割 | Int キーの `sortBy`、`sortWith`、`subList` だけの提出にも `KeySort.kt`(約 2 KB)が入る。`sortByLong` などを別ファイルに分けると減らせる。節約が小さいので急がない | なし |
| コンパイル時間とソース長の上限の調査 | 後回し(2026-11-07 に決定、理由は §15)。Phase 4 でツリーを入れるときか、実際の提出が大きくなったときに行う | コードテスト(`e2e/experiments/size-probe.py`) |

### Phase 3: ハッシュ

`HashSet`、`HashMap`。負荷率、ハッシュ関数、キー管理の実測と決定。

**生成器の準備(最初の `HashMap` のテンプレートより先に行う)**

今の生成器では `KTypeVType*` のテンプレートがすべて同じ組み合わせで展開されるので、`HashMap` に必要なクラスを生成できません。利用者がペアヒープだけの間は作っても確かめられないので、ここで行います。

- `Codegen.GENERATED_PAIRS` をやめ、キーと値の組み合わせをテンプレートごとに持たせる。§5.1 の `HashMap`(`Obj`-`Obj` を除く 19 クラス)と `TreeMap` の組み合わせを表せるようにする
- 値に `Double` を、キーに `Obj` を追加する(`VALUE_SPECS` と `TYPE_SPECS`)
- `CodegenTest` の期待値に各コレクションの組み合わせを加え、§5.1 と一致することを確かめる

**完了条件**: 差分テストと劣化ケースのテストの通過。HPPC / fastutil との比較。

### Phase 4: ツリー

AVL / WBT と B+ 木の比較実験(Int キーのみで実装)、方式の決定、`TreeSet`、`TreeMap`、`TreeMultiset`。

**完了条件**: `java.util.TreeMap` に対する差分テスト、順序統計の参照モデル(ソート済み配列)との一致、代表問題の AC。

### Phase 5: 周辺機能

`IntPair`、`FixedBitSet`、`FastSet`、`CsrGraph`、座標圧縮。

### Phase 6: 1.0 リリース

- API の見直し。互換性の保証はせず、必要なら変更します(凍結はジャッジ導入時のみ。導入後は長期間修正できないため)
- インストールスクリプト案(`install/`)を公開
- 自分が使うための最小限の文書: API の注意点を書いた KDoc と短い README(ライセンス、AI ルールの URL 添付条件、サポートしない旨)
- GitHub Release に jar と SHA-256 を公開。ジャッジへの導入はこの jar を使います(`ac-library-java` と同じ方式)
- タグは参照 URL の根拠になるので、付け替えや削除をしません

### Phase 7: ジャッジへのインストール提案(次回の言語アップデート時)

本プロジェクトの最終目標です。準備は Phase 1 から続けます(§1.4)。

**常時行うこと**

- インストールスクリプト案のリハーサルを CI で実行し、jar のサイズ、コンパイル時間、起動時間の影響を毎リリース記録
- 実際に使った問題と効果(`java.util` 版との時間・メモリ比較)を蓄積し、提案の根拠にする
- 言語アップデートの告知を確認(時期は不明なので、告知があればすぐ動けるようにしておく)

**アップデートの告知後に行うこと**

1. Kotlin / JDK の設定を新環境に変更し、ジャッジ再現環境を新しい TOML から作り直して全ゲートを実行
2. API を凍結して、新環境向けの jar をリリース
3. その時点の既存エントリの形式に合わせて TOML を作成し、提案(jar の取得、SHA-256 の検証、`-cp` への追加)
4. コンパイル時間と起動時間への影響のデータを添える(他の利用者が不利にならないことを示す)

導入は保証されないので、見送られた場合はバンドラでの運用を続け、次の機会に再提案します。

---

## 12. リポジトリ構成(案)

```
.
├── LICENSE, README.md, THIRD_PARTY_NOTICES
├── build-logic/          # 生成器(Gradle included build)
├── templates/            # 本体テンプレート(正しい Kotlin)
├── generated/            # 生成済みソース(コミットする。§9)
├── src/main/kotlin/      # 手書きコード(FixedBitSet、IntPair など)
├── src/test/kotlin/      # 手書きテスト、テスト基盤
├── test-templates/       # 全型展開するテスト
├── src/jmh/              # ベンチマーク
├── bytecode-check/       # ASM によるボクシング検査
├── bundler/              # 貼り付け用バンドラ CLI
├── e2e/                  # Docker、利用コード、代表問題、計画の根拠にした実験コード
├── install/              # ジャッジへのインストールスクリプト案
├── docs/                 # 本計画書、judge-env.md、設計判断の記録
└── .github/workflows/    # PR と夜間の CI
```

### CI

| タイミング | 内容 |
|---|---|
| PR | 生成の決定性と `generated/` の一致、ビルド、差分テスト(短)、カバレッジ、ボクシング検査、確保量、禁止パターン、`reuse lint`、E2E |
| 夜間 | 差分テスト(長)、PIT、JMH の回帰検知 |
| リリース | 全ゲート、jar とソースの公開、バンドラ出力の検証 |

---

## 13. 判断事項

### 決定済み

| 事項 | 決定 |
|---|---|
| ライセンス | CC0-1.0 |
| リポジトリ | GitHub で公開(AI ルールの条件を満たすため。Maven などへの公開はしない) |
| 想定利用者 | 作者本人のみ。他者の利用は妨げないがサポートしない |
| 標準コレクションとの相互運用 | 変換(コピー)のみ提供し、`List<Int>` などは実装しない(§4.6) |
| 可変と不変 | 全コレクションを可変のみとする(§4.7) |
| `subList` | ボクシングなしのビューとして提供する(§5.3.1) |
| Kotlin の演算子と慣用表現 | `[]`、`in`、`+` などを一通りサポートする(§4.8) |
| リポジトリ名 | `ac-primitive-collections-kt` |
| パッケージ名 | `ac_primitive_collections`(`import ac_primitive_collections.*`)。ジャッジ導入済みの `ac_library` と同じ形式 |
| 対象の型 | Int、Long、Double、Char のみ。`Float`、`Short`、`Byte`、`Boolean` は使わないので作らない。`Char` のヒープも作らない(§5.1) |
| `map[k]` の不在時 | ゼロ値を返す。存在確認は `k in map` などで可能(§4.3) |
| 生成コードの公開 | `generated/` にコミットし、テンプレートとの一致を CI で検査する(§9) |
| Gradle | 8.14.3。Kotlin Gradle プラグイン 2.2.10 の対応範囲に合わせる(§11 Phase 0) |
| 対象環境 | 当面は現行ジャッジ(Kotlin 2.2.10、JDK 23.0.2)。提出は貼り付け方式 |
| ジャッジへの導入 | **次回の言語アップデートで提案する(最終目標、§1.4)**。時期は未定 |

### その他の判断

| 事項 | 採用する案 | 不採用の案 |
|---|---|---|
| ハッシュのシード | 決定的 + インスタンス別ソルト | プロセスごとに乱数(再現性を失う) |
| `UInt` / `ULong` | v1 では対象外 | 最初から含める |

---

## 14. リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| ジャッジへの導入が見送られる、または数年先になる | 最終目標が遅れ、貼り付けの不便さが続く | バンドラで使い続けられるようにしつつ、実績データを蓄積して次の機会に再提案 |
| 導入の提案が受け入れられにくい(全提出のコンパイル時間やクラスパスに影響) | 導入されない | jar を小さく保ち、依存をなくし、影響の計測値を添える |
| バンドルでソース長やコンパイル時間が上限に近づく | 提出できない、または CE | クラス単位の削減、サイズ予算を CI で監視、大きな提出が現実になったときに実測(§15) |
| 貼り付けと `import` で名前の解決が異なる | コンテスト中に CE、移行時の修正 | バンドラの衝突検出、名前の衝突検査、両方式の E2E(§8.3) |
| 生成 AI ルールの改定 | 使用が違反になる可能性 | ルールの定期確認、URL 添付と事前公開を維持 |
| 次回アップデートで Kotlin / JDK が変わる | 互換性の破損、最適化の前提の変化 | バージョン設定を 1 か所に集約し、新しい TOML から環境を再現して全ゲートと性能の判断をやり直す |
| `map[k]` がゼロ値を返すため、キーの有無の取り違えに気づきにくい | 誤答 | KDoc と README で明記、`k in map` と `findSlot` を提供 |
| JMH と実問題で傾向が異なる | 誤った最適化 | 実プロセスの 1 回実行と AtCoder 実機で最終判断 |
| 本番のジャッジの CPU がコードテストと異なる(AVX-512 なしなど) | `Arrays.sort` が想定より遅くなる | 正しさには影響しない。次回の言語アップデートで計測し直す |
| テンプレートの一括バグ | 全型に波及 | 全型での差分テスト、参照モデルを実装から独立 |
| 型行列の肥大化 | コンパイルや開発の遅延 | サイズ予算を CI で監視、型行列(§5.1)を見直す |
| B+ 木の実装の複雑さ | Phase 4 の遅延 | AVL を先に完成させ、差し替え可能にする |

---

## 15. 未確認事項

| 項目 | 状況 | 確認時期 |
|---|---|---|
| コンパイル時間の上限 | 未確認。公式の記載は見つからない(下記)。増え方の参考値は `docs/judge-env.md` | 後回し。Phase 4 でツリーを入れるときか、実際の提出が大きくなったとき(`e2e/experiments/size-probe.py` のファイルをコードテストで確認。手作業なので依頼は最小限にする) |
| ソース長の上限 | 未確認。公式の記載は見つからない(下記)。512 KiB は個人の記事で見つけた値で、公式の裏付けはない | 同上 |
| `nextCapacity` の拡張率 | 暫定で 1.5 倍 | 未実施。Phase 2 以降(`e2e/experiments/GrowthRate.kt`) |
| `java.util` との 1 回実行の比較(S5) | `IntList` は 10^6 要素で 2.1 倍速い(`docs/judge-env.md` §6)。`LongIntMinHeap` は実行時間全体で 1.4 倍速い(同 §7)。他のコレクションは各 Phase で確認 | 各 Phase |
| メモリの目標(`java.util` 比 1/3 以下) | 未確認。プロセス全体では 0.67 倍だが JVM の基礎分を含む | Phase 2 |
| `add` の n = 10^6 での速度 | `modCount` の有無と関係なく、同じ構造の比較クラスより約 1.5 倍遅い(0.40 と 0.55〜0.68 ops/ms)。原因は未解明。`ArrayList`(0.25)には勝つ。値は arm64 の JMH だけで、判断には使えない(§7.6) | x64 の CI で JMH を確認するとき |
| コードテストと本番のジャッジが同じ構成か | 未確認。コードテストの実測は `docs/judge-env.md` | 確認手段なし |
| `Arrays.sort` の SIMD 実装が実際に使われているか | 直接は未確認。実機の速度は SIMD の利用と整合する | 確認手段なし |
| §5〜§7 の性能に関する見込みと目標値 | 仮説。実測後に確定 | 各 Phase。B+ 木の速度差は Phase 4 |

**コンパイル時間とソース長の上限を後回しにした理由(2026-11-07)**

- バンドラは使うクラスだけを結合するので、全型の `List` を追加しても、使わない提出は大きくならない。
- 実際の問題に近い E2E の貼り付けコードは約 10〜47 KB。ペアヒープの貼り付け提出は ABC 340 D で AC している(約 10 KB)。4 型と変換を 1 つの提出で使う最悪に近いサンプル(`e2e/usage/lists-other-types`)は約 163 KB。
- 問題になるとすれば、1 つの提出で大きなクラスをいくつも使う場合で、将来の `TreeMap` などが該当しそう。上限がわからなくても、今の作業は変わらない。
- ルールによると CE にはペナルティがつかない。ただし、上限を超えたときに CE 扱いになるかは不明。

**公式情報の確認結果(2026-11-07)**

次のページに、ソース長とコンパイル時間の上限の記載は見つからなかった。コンパイルが長すぎた場合の扱いの記載も見つからなかった。

- 全文を確認: [FAQ(日本語)](https://atcoder.jp/faq?lang=ja)、[FAQ(英語)](https://atcoder.jp/faq?lang=en)、[practice contest のトップ](https://atcoder.jp/contests/practice?lang=ja)、[新ジャッジ運用開始のお知らせ](https://atcoder.jp/posts/1579)、[Kotlin install TOML](https://img.atcoder.jp/file/language-update/2025-10/062-2-0-21_openjdk-23.toml)
- 一部は取得ツールの要約で確認: [practice contest のルール](https://atcoder.jp/contests/practice/rules)、[ABC 428 のルール](https://atcoder.jp/contests/abc428/rules)、[Language Test 202505 のルール](https://atcoder.jp/contests/language-test-202505/rules)、[言語一覧](https://img.atcoder.jp/file/language-update/2025-10/language-list.html)
- 未確認: 提出画面とコードテストの画面。ログインが必要で、自動取得では読めなかった

512 KiB の値は [Brainf**k の処理系を扱った Zenn の記事](https://zenn.dev/angel_p_57/articles/d7e2f3529c801d)にある。

---

## 16. 出典

- [AtCoder 言語一覧 2025/10 版](https://img.atcoder.jp/file/language-update/2025-10/language-list.html)
- [Kotlin install TOML](https://img.atcoder.jp/file/language-update/2025-10/062-2-0-21_openjdk-23.toml)
- [Language Test 202505](https://atcoder.jp/contests/language-test-202505)、[ルール](https://atcoder.jp/contests/language-test-202505/rules)
- [AtCoder 生成 AI 対策ルール 20261014 版](https://info.atcoder.jp/entry/llm-rules-ja)
- [ABC 428 ルール](https://atcoder.jp/contests/abc428/rules)、[お問い合わせ](https://atcoder.jp/contact?lang=ja)、[新ジャッジ運用開始のお知らせ](https://atcoder.jp/posts/1579)
- [Kotlin Gradle プラグインの対応表](https://kotlinlang.org/docs/gradle-configure-project.html)、[Gradle の Java 対応表](https://docs.gradle.org/current/userguide/compatibility.html)
- [JDK-8319577: AVX2 intrinsics for Arrays.sort(統合)](https://mail.openjdk.org/pipermail/build-dev/2023-December/042482.html)、[JDK-8309130: AVX-512](https://mail.openjdk.org/pipermail/build-dev/2023-September/040816.html)
- [Dual-Pivot Quicksort 最終版の告知](https://mail.openjdk.org/pipermail/core-libs-dev/2019-June/060915.html)
- jar サイズ: Maven Central の HTTP ヘッダで確認(fastutil 8.5.15、hppc 0.10.0、eclipse-collections 11.1.0)
