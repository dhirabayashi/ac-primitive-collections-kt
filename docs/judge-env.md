# ジャッジ環境の実測記録

## 1. AtCoder コードテスト(`e2e/judge-probe/Main.kt`)

計測日: 2026-11-05。1 回の実行結果です。

コードテストと本番のジャッジが同じマシン構成かは確認していません。

### 環境

| 項目 | 値 |
|---|---|
| Kotlin / Java | 2.2.10 / 23.0.2 |
| OS / アーキテクチャ | Linux / amd64 |
| CPU | Intel Xeon Platinum 8488C(AVX、AVX2、AVX-512F、AVX-512DQ、BMI2) |
| `UseAVX` | 3(AVX-512 まで有効) |
| プロセッサ数 | **1** |
| GC | **Serial**(`Copy`、`MarkSweepCompact`) |
| `MaxHeapSize` | 1000341504 バイト(954 MiB) |
| `maxMemory()` | **922 MiB** |
| `InitialHeapSize` | 62914560 バイト(60 MiB) |
| 物理メモリ | 3905688 kB(`MemTotal`) |
| JVM 引数 | `-Xss1024M -DONLINE_JUDGE=true` |
| `CICompilerCount` | 2 |
| `DontCompileHugeMethods` | true |
| cgroup の上限 | 取得できず |

`MaxHeapSize` は物理メモリの約 1/4 で、JVM の既定値と一致します。

### ソート時間(ランダムデータ)

`first` は 1 回目、`min` は複数回の最小値です(200,000 要素は 5 回、5,000,000 要素は 3 回)。5,000,000 要素は 200,000 要素の後に実行しているので、`first` はコールドスタートの値ではありません。

| 対象 | 200,000 要素 first / min | 5,000,000 要素 first / min |
|---|---|---|
| `IntArray`、`Arrays.sort` | 51 / 5 ms | 170 / 37 ms |
| `LongArray`、`Arrays.sort` | 53 / 4 ms | 78 / 74 ms |
| `LongArray`、8 bit 基数ソート | 62 / 5 ms | 167 / 167 ms |
| `DoubleArray`、`Arrays.sort` | 60 / 4 ms | 112 / 76 ms |

### 生ログ

```
kotlin=2.2.10 java=23.0.2 vm=OpenJDK 64-Bit Server VM
os=Linux arch=amd64
args=[-Xss1024M, -DONLINE_JUDGE=true]
processors=1 maxMemoryMiB=922
gc=[Copy, MarkSweepCompact]
vm.UseAVX=3
vm.UseSSE=4
vm.MaxHeapSize=1000341504
vm.InitialHeapSize=62914560
vm.ThreadStackSize=1048576
vm.CICompilerCount=2
vm.TieredCompilation=true
vm.DontCompileHugeMethods=true
cpu=Intel(R) Xeon(R) Platinum 8488C
cpuFlags=[avx, avx2, avx512f, avx512dq, bmi2]
memTotal=MemTotal:        3905688 kB
cgroupMemoryMax=null
cgroupCpuMax=null
sort label=int-Arrays.sort n=200000 firstMs=51 minMs=5
sort label=long-Arrays.sort n=200000 firstMs=53 minMs=4
sort label=long-radix8 n=200000 firstMs=62 minMs=5
sort label=double-Arrays.sort n=200000 firstMs=60 minMs=4
sort label=int-Arrays.sort n=5000000 firstMs=170 minMs=37
sort label=long-Arrays.sort n=5000000 firstMs=78 minMs=74
sort label=long-radix8 n=5000000 firstMs=167 minMs=167
sort label=double-Arrays.sort n=5000000 firstMs=112 minMs=76
```

## 2. ヒープ上限の再現実験(ローカル)

コードテストの値に合わせて、ローカルの JDK 23(arm64)で計測しました。

```
-XX:+UseSerialGC -XX:MaxHeapSize=1000341504 -XX:InitialHeapSize=62914560 -XX:ActiveProcessorCount=1 -Xss1024M
```

`maxMemory()` はローカルで 928 MiB、コードテストで 922 MiB と少し異なるので、以下は目安です。

| 計測 | 結果 |
|---|---|
| 1 個の配列として確保できる最大サイズ(Int、Long とも) | 約 637 MiB |
| `IntArray` を 2 倍ずつ拡張 | 271 MiB(約 7100 万要素)まで。543 MiB への拡張で失敗 |
| `IntArray` を 1.5 倍ずつ拡張 | 331 MiB(約 8700 万要素)まで。497 MiB への拡張で失敗 |
| `IntArray` を 1.25 倍ずつ拡張 | 345 MiB(約 9000 万要素)まで。431 MiB への拡張で失敗 |

## 3. コンパイル時間の参考値(ローカル)

Apple M1、1 プロセッサ、ヒープ 512 MB で、単純なクラスを繰り返した合成コードをコンパイルした値です。別のエージェントによる検証時の計測で、再現は確認していません。ジャッジの値ではないので、増え方の目安にとどめます。

| ソースサイズ | コンパイル時間 |
|---|---|
| 最小(`main` のみ) | 2.5 秒 |
| 32 KiB | 4.8 秒 |
| 128 KiB | 7.4 秒 |
| 512 KiB | 13.9 秒 |

## 4. ジャッジ再現環境(Docker、arm64 上の x64 エミュレーション)

計測日: 2026-11-06。`--cpus=1 --memory=3900m` で実行しました。`--cpus` を指定しないと、JVM は 8 プロセッサと G1 を選びます。**時間は Apple Silicon 上のエミュレーションの値で、ジャッジの値ではありません。** 増え方の目安にとどめます。

| 対象 | ソースサイズ | コンパイル時間 |
|---|---|---|
| `abc231c`(jar 方式) | 584 バイト | 11 秒 |
| `abc231c`(貼り付け) | 26.6 KB | 22〜26 秒 |
| `intlist-all`(jar 方式) | 4.4 KB | 16〜18 秒 |
| `intlist-all`(貼り付け) | 42.9 KB | 29 秒 |
| `size-probe.py` の出力 | 約 74 KB | 32 秒 |
| 同上 | 約 285 KB | 51 秒 |
| 同上 | 約 544 KB | 70 秒 |

`IntList` を 1 つ貼るだけでコンパイル時間が約 2 倍になりました。実機での上限の確認が必要です。

## 5. JMH(ローカル arm64、Serial GC、1 プロセッサ)

計測日: 2026-11-06。回帰の検知と傾向の確認用で、判断の根拠にはしません(計画書 §7.6)。

| `add`(ops/ms) | n = 1000 | n = 10^6 |
|---|---|---|
| `IntList` | 757 | 0.395 |
| `modCount` なしの比較クラス | 837 | 0.554 |
| `modCount` ありの比較クラス(先頭で加算) | 388 | 0.680 |
| `ArrayList<Int>` | 429 | 0.254 |

## 6. `IntList` と `ArrayList<Int>` の比較(AtCoder コードテスト)

計測日: 2026-11-06。`e2e/experiments/compare-arraylist/Main.kt`、10^6 要素、各 3 回。1 回ごとに別の JVM です。処理は追加、ランダムアクセス、全要素の走査、ソート、二分探索で、値は乱数です。両者の計算結果は一致しました。

| 指標 | `IntList` | `ArrayList<Int>` | 平均の比 |
|---|---|---|---|
| 処理時間 `ms`(JVM の起動を除く) | 290、366、356 | 754、889、828 | 2.4 倍速い |
| 実行時間(ms) | 414、463、460 | 854、984、914 | 2.1 倍速い |
| メモリ(KiB) | 57204、56956、57120 | 85564、85604、85280 | 0.67 倍 |

メモリは JVM 自体の分を含むプロセス全体の値です。この JVM の基礎分は測っていないので、「`java.util` 比で 1/3 以下」(計画書 §7.6)を満たすかは、この値では判断できません。

## 7. 分かったこと

1. **Long / Double の自然順ソートは `Arrays.sort` を使います。** 5,000,000 要素の Long で `Arrays.sort` は基数ソートの 2 倍以上速く、採用条件を満たしません。ローカル(arm64)の結果とは逆でした。
2. **コールドスタートの影響が大きいです。** 200,000 要素の 1 回目は最小値の約 10 倍かかります。プロセッサが 1 個なので、JIT コンパイルと本体が同じ CPU を取り合います。1 回実行の時間を重視して計測します。
3. **使えるヒープは約 922 MiB です。** 問題のメモリ制限が大きくても、`-Xmx` がないので増えません(物理メモリが同じ場合)。
4. **配列の拡張は `maxMemory()` よりかなり手前で失敗します。** 新旧の配列が同時に必要で、1 個の配列も約 637 MiB までです。
5. **並列化の効果はありません。** Serial GC とプロセッサ 1 個の環境です。
