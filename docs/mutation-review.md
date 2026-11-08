# 変異テストの確認記録

`./gradlew pitest` で生存した変異を 1 件ずつ確認した記録です(§7.1)。

## 結果(4 型の `List`、`SubList`、型変換、ペアヒープ 18 クラス)

2026-11-08 の 1 回目の実行の結果です。ゲートを変えた後の 2 回目の実行では、TIMED_OUT が 149 件、SURVIVED が 251 件になりました(`docs/mutation-gate.md` §5)。生存変異は、3 回の実行のどれでも「確認済みの生存変異」の表に収まっています(2 回目と 3 回目は `pitestBaseline` で確認)。

| 状態 | 件数 |
|---|---|
| KILLED | 3426 |
| TIMED_OUT(検出扱い) | 151 |
| MEMORY_ERROR(検出扱い) | 3 |
| SURVIVED | 249 |
| スコア | 93.50%(参考値。ゲートには使わない) |

ペアヒープの生存変異は 18 クラスとも同じ 10 件で、キーの型、値の型、向きによる違いはありません(`mutations.xml` を集計して確認)。`List`、`SubList`、ソートの生存変異は 4 型とも「確認済みの生存変異」の表の箇所に収まり、新しく追加した `KeySort.kt` には生存変異がありません。`List` の件数は 6〜12 件(`Long` 6、`Double` 7、`Int` 11、`Char` 12)と型で異なりますが、同じ変異が型や実行によって `SURVIVED` と `TIMED_OUT` のどちらにもなるためです。この実行では、`removeLast` の `modCount++` が `LongList` と `CharList` では `SURVIVED`、`IntList` と `DoubleList` では `TIMED_OUT` でした。`removeAt` の境界の変異は、`IntList` と `CharList` では `SURVIVED`、`LongList` と `DoubleList` では `TIMED_OUT` でした。そのためスコアも実行ごとに少し変わります(同じコードで 0.1 ポイント程度)。

**ゲートは検出率ではなく、下の「確認済みの生存変異」の表です**(2026-11-08 に変更。経緯は `docs/mutation-gate.md`)。等価変異は生成したクラスの数だけ数えられるので、検出率はテストの質と関係なく、クラスが増えるほど下がります。新しい生存変異が出たら、テストを足すか、等価だと確認して表に足します。`modCount++` の変異を消すためだけに `modCount` の値を検査するテストは、実装の細部に縛られるだけなので追加していません。

## PIT の対象外

- **inline 関数の本体**(`forEach`、`map`、`fold`、`removeIf`、`sortByLong`、`mapToLong` など): 実行されるのは呼び出し側に展開されたコピーなので、元のメソッドを変異させても観測できません。代わりに差分テストと分岐カバレッジ 100% で確認します。**変異テストで確かめられていない部分**であり、残るリスクとして扱います。
- `kotlin.jvm.internal` の呼び出し: コンパイラが生成する null 検査です。

## 確認済みの生存変異

**この表はゲートの正本です。** `./gradlew pitest` の後に `pitestBaseline` が動き、検出されなかった変異(`detected="false"`)を表と照合します。表にない変異が 1 件でもあるか、1 つの生成クラスで件数が「上限」を超えると失敗します。照合の仕組みと、この形にした理由は `docs/mutation-gate.md` にあります。

照合に使うのは、テンプレート、メソッド、ミューテータ、説明、上限の 5 列です。

- **テンプレート:** 生成ファイルの 2 行目のヘッダから取ります。ペアヒープの 18 クラスは `KTypeVTypeMinHeap.kt` の 1 行で表します。手書きのファイルは、ファイル名をそのまま書きます。
- **メソッド:** 型名を `KType`、`VType` に戻した名前です(`mergeSortLong` は `mergeSortKType`)。1 つのセルに複数書けて、それぞれに同じ上限が付きます。
- **説明:** PIT の説明をそのまま書きます。値の型の語(`integer`、`long` など)は変えません。生成したクラスの名前だけは、テンプレートの名前に戻して書きます。
- **上限:** 1 つの生成クラスで生き残ってよい件数です。型ごとの件数の違いは、`SURVIVED` と `TIMED_OUT` の差によるものなので、型の最大値を書きます。

「変異」の列にある行番号は、`generated/main/kotlin/ac_primitive_collections/` の `Int` 版のものです。照合には使いません。`Long`、`Double` の `List` も同じ行で、`Char` は `sum` がない分だけ以降の行がずれます。

| テンプレート | メソッド | ミューテータ | 説明 | 上限 | 変異 | 判定の理由 |
|---|---|---|---|---|---|---|
| `KTypeList.kt` | `add`、`clear`、`finishRemoveIf`、`openGap`、`removeLast`、`removeRange`、`swapRemove`、`truncate` | `MathMutator` | `Replaced integer addition with subtraction` | 1 | `modCount++` を `modCount--` に(73、205、191、741、129、145、154、200 行) | `modCount` は変化の有無だけを比較するので等価 |
| `KTypeList.kt` | `add` | `MathMutator` | `Replaced long addition with subtraction` | 1 | `n + 1L` を `n - 1L` に(70 行) | `grow` は容量満杯のときだけ呼ばれ、`nextCapacity` は最低でも 1.5 倍(下限 8)を返すので結果が同じ |
| `KTypeList.kt` | `removeAt` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `index >= length` を `>` に(116 行) | `index == length` は続く `removeRange` の範囲検査で同じ例外になる |
| `KTypeList.kt` | `resize` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `newSize <= old` を `<` に(221 行) | 等しいときは長さ 0 の `openGap` になり、何も起きない |
| `KTypeList.kt` | `reverse` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `i < j` を `<=` に(331 行) | 中央の要素を自分自身と交換するだけ |
| `KTypeList.kt` | `shuffle` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `i > from` を `>=` に(346 行) | 乱数の消費が 1 回増えるだけで、交換は自分自身と。分布は変わらない |
| `KTypeListConversions.kt` | `addAll` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 2 | コンパイラが生成する `step > 0` を `>= 0` に、`step < 0` を `<= 0` に(44 行) | プログレッションの `step` は 0 にならないので等価 |
| `KTypeSubList.kt` | `binarySearch` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `found >= 0` を `>` に(80 行) | `found == 0` は `offset == 0` のときだけで、両方の式が 0 になる |
| `KTypeListSort.kt` | `mergeSortKType` | `MathMutator` | `Replaced integer subtraction with addition` | 1 | `to - from` を `to + from` に(77 行) | `n` は作業領域の大きさと打ち切りの判定にのみ使う。実際の添字は `to` で制限されるので結果は同じ(余分な確保のみ) |
| `KTypeListSort.kt` | `mergeSortKType` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 5 | 挿入ソートとマージの境界(80、85、96 行)、作業領域の計算の境界(89 行)、打ち切りの境界(101 行) | 挿入ソートとマージはラン長が 1 違うだけで結果は同じ。作業領域は大きくなるだけ。打ち切りは何もしないパスが 1 回増えるだけ |
| `KTypeListSort.kt` | `mergeKType` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `<= 0` を `< 0` に(マージ省略の判定、121 行) | 省略しなくても安定マージの結果は同じ |
| `KTypeVTypeMinHeap.kt` | `add`、`removeFirst`、`replaceFirst`、`clear` | `MathMutator` | `Replaced integer addition with subtraction` | 1 | `modCount++` を `modCount--` に | `KTypeList` と同じく、`modCount` は変化の有無だけを比較するので等価 |
| `KTypeVTypeMinHeap.kt` | `add` | `MathMutator` | `Replaced long addition with subtraction` | 1 | `n + 1L` を `n - 1L` に | `KTypeList` の `add` と同じ理由で結果が同じ |
| `KTypeVTypeMinHeap.kt` | `removeFirst` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | `last > 0` を `>= 0` に | 空になるときの `siftDown` は、先頭に自分自身を書き戻すだけ。`Obj` ではその後に同じ位置を `null` にする |
| `KTypeVTypeMinHeap.kt` | `siftUp` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | 比較の `<` を `<=` に | 等しいキーの間の順序だけが変わる。この順序は不定と定めている。`Double` の等しさは全順序で判定する |
| `KTypeVTypeMinHeap.kt` | `siftDown` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 3 | 比較の `<` を `<=` に(2 件)、子の範囲の `>` を `>=` に(1 件) | 比較は `siftUp` と同じ理由。子の範囲は、ちょうど 4 個のときはどちらの式も同じ値になる |
