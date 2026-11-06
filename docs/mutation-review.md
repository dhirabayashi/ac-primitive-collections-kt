# 変異テストの確認記録

`./gradlew pitest` で生存した変異を 1 件ずつ確認した記録です(§7.1)。

## 結果(Phase 1、`IntList` と `IntSubList`)

| 状態 | 件数 |
|---|---|
| KILLED | 527 |
| TIMED_OUT(検出扱い) | 14 |
| MEMORY_ERROR(検出扱い) | 1 |
| SURVIVED | 21 |
| スコア | 96%(基準 90%) |

## PIT の対象外

- **inline 関数の本体**(`forEach`、`map`、`fold`、`removeIf` など): 実行されるのは呼び出し側に展開されたコピーなので、元のメソッドを変異させても観測できません。代わりに差分テストと分岐カバレッジ 100% で確認します。**変異テストで確かめられていない部分**であり、残るリスクとして扱います。
- `kotlin.jvm.internal` の呼び出し: コンパイラが生成する null 検査です。

## 生存した変異(すべて等価、または性能のみに影響)

行番号は `generated/main/kotlin/ac_primitive_collections/` のものです。

| 箇所 | 変異 | 判定の理由 |
|---|---|---|
| `IntList.kt` `add` 73、`clear` 205、`finishRemoveIf` 191、`openGap` 733、`removeLast` 129、`removeRange` 145、`swapRemove` 154、`truncate` 200 | `modCount++` を `modCount--` に | `modCount` は変化の有無だけを比較するので等価 |
| `IntList.kt` `add` 70 | `n + 1L` を `n - 1L` に | `grow` は容量満杯のときだけ呼ばれ、`nextCapacity` は最低でも 1.5 倍(下限 8)を返すので結果が同じ |
| `IntList.kt` `removeAt` 116 | `index >= length` を `>` に | `index == length` は続く `removeRange` の範囲検査で同じ例外になる |
| `IntList.kt` `resize` 221 | `newSize <= old` を `<` に | 等しいときは長さ 0 の `openGap` になり、何も起きない |
| `IntList.kt` `reverse` 331 | `i < j` を `<=` に | 中央の要素を自分自身と交換するだけ |
| `IntList.kt` `shuffle` 346 | `i > from` を `>=` に | 乱数の消費が 1 回増えるだけで、交換は自分自身と。分布は変わらない |
| `IntSubList.kt` `binarySearch` 80 | `found >= 0` を `>` に | `found == 0` は `offset == 0` のときだけで、両方の式が 0 になる |
| `IntListSort.kt` `mergeSortInt` 58 | `to - from` を `to + from` に | `n` は作業領域の大きさと打ち切りの判定にのみ使う。実際の添字は `to` で制限されるので結果は同じ(余分な確保のみ) |
| `IntListSort.kt` `mergeSortInt` 61、66、77 | 挿入ソートとマージの境界 | ラン長が 1 違うだけで結果は同じ |
| `IntListSort.kt` `mergeSortInt` 70 | 作業領域の計算の境界 | 作業領域が大きくなるだけ |
| `IntListSort.kt` `mergeSortInt` 82 | 打ち切りの境界 | 何もしないパスが 1 回増えるだけ |
| `IntListSort.kt` `mergeInt` 102 | `<= 0` を `< 0` に(マージ省略の判定) | 省略しなくても安定マージの結果は同じ |
