# 変異テストの確認記録

`./gradlew pitest` で生存した変異を 1 件ずつ確認した記録です(§7.1)。

## 結果(4 型の `List`、`SubList`、型変換、ペアヒープ 18 クラス)

| 状態 | 件数 |
|---|---|
| KILLED | 3376 |
| TIMED_OUT(検出扱い) | 139 |
| SURVIVED | 254 |
| スコア | 93.26%(基準 90%) |

ペアヒープの生存変異は 18 クラスとも同じ 10 件で、キーの型、値の型、向きによる違いはありません(`mutations.xml` を集計して確認)。`List`、`SubList`、ソートの生存変異は 4 型とも下の表の箇所に収まり、新しく追加した `KeySort.kt` には生存変異がありません。`List` の件数は 8〜13 件と型で異なりますが、同じ変異が実行ごとに `SURVIVED` と `TIMED_OUT` のどちらにもなるためです(`removeLast` の `modCount++` は、`Int` と `Double` では `TIMED_OUT`、`Long` と `Char` では `SURVIVED`)。

**基準との差が小さいことに注意してください。** 等価変異が多いテンプレートを増やすと、テストの質が変わらなくても基準を下回ることがあります。その場合は基準を下げる前に、生存変異が等価かどうかを確認します。`modCount++` の変異を消すためだけに `modCount` の値を検査するテストは、実装の細部に縛られるだけなので追加していません。

## PIT の対象外

- **inline 関数の本体**(`forEach`、`map`、`fold`、`removeIf`、`sortByLong`、`mapToLong` など): 実行されるのは呼び出し側に展開されたコピーなので、元のメソッドを変異させても観測できません。代わりに差分テストと分岐カバレッジ 100% で確認します。**変異テストで確かめられていない部分**であり、残るリスクとして扱います。
- `kotlin.jvm.internal` の呼び出し: コンパイラが生成する null 検査です。

## 生存した変異(すべて等価、または性能のみに影響)

行番号は `generated/main/kotlin/ac_primitive_collections/` のものです。`Long`、`Double` の `List` も同じ行で、`Char` は `sum` がない分だけ以降の行がずれます。ペアヒープは 18 クラスに共通なので、`KTypeVTypeMinHeap` として書きます。

| 箇所 | 変異 | 判定の理由 |
|---|---|---|
| `IntList.kt` `add` 73、`clear` 205、`finishRemoveIf` 191、`openGap` 733、`removeLast` 129、`removeRange` 145、`swapRemove` 154、`truncate` 200 | `modCount++` を `modCount--` に | `modCount` は変化の有無だけを比較するので等価 |
| `IntList.kt` `add` 70 | `n + 1L` を `n - 1L` に | `grow` は容量満杯のときだけ呼ばれ、`nextCapacity` は最低でも 1.5 倍(下限 8)を返すので結果が同じ |
| `IntList.kt` `removeAt` 116 | `index >= length` を `>` に | `index == length` は続く `removeRange` の範囲検査で同じ例外になる |
| `IntList.kt` `resize` 221 | `newSize <= old` を `<` に | 等しいときは長さ 0 の `openGap` になり、何も起きない |
| `IntList.kt` `reverse` 331 | `i < j` を `<=` に | 中央の要素を自分自身と交換するだけ |
| `IntList.kt` `shuffle` 346 | `i > from` を `>=` に | 乱数の消費が 1 回増えるだけで、交換は自分自身と。分布は変わらない |
| `IntSubList.kt` `binarySearch` 80 | `found >= 0` を `>` に | `found == 0` は `offset == 0` のときだけで、両方の式が 0 になる |
| `IntListSort.kt` `mergeSortInt` 85 | `to - from` を `to + from` に | `n` は作業領域の大きさと打ち切りの判定にのみ使う。実際の添字は `to` で制限されるので結果は同じ(余分な確保のみ) |
| `IntListSort.kt` `mergeSortInt` 88、93、104 | 挿入ソートとマージの境界 | ラン長が 1 違うだけで結果は同じ |
| `IntListSort.kt` `mergeSortInt` 97 | 作業領域の計算の境界 | 作業領域が大きくなるだけ |
| `IntListSort.kt` `mergeSortInt` 109 | 打ち切りの境界 | 何もしないパスが 1 回増えるだけ |
| `IntListSort.kt` `mergeInt` 129 | `<= 0` を `< 0` に(マージ省略の判定) | 省略しなくても安定マージの結果は同じ |
| `KTypeVTypeMinHeap` `add`、`removeFirst`、`replaceFirst`、`clear` | `modCount++` を `modCount--` に | `IntList` と同じく、`modCount` は変化の有無だけを比較するので等価 |
| `KTypeVTypeMinHeap` `add` | `n + 1L` を `n - 1L` に | `IntList.add` と同じ理由で結果が同じ |
| `KTypeVTypeMinHeap` `removeFirst` | `last > 0` を `>= 0` に | 空になるときの `siftDown` は、先頭に自分自身を書き戻すだけ。`Obj` ではその後に同じ位置を `null` にする |
| `KTypeVTypeMinHeap` `siftUp`、`siftDown` の比較 | `<` を `<=` に | 等しいキーの間の順序だけが変わる。この順序は不定と定めている。`Double` の等しさは全順序で判定する |
| `KTypeVTypeMinHeap` `siftDown` の子の範囲 | `>` を `>=` に | ちょうど 4 個のときはどちらの式も同じ値になる |
