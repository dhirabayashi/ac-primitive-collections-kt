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

## 4. 分かったこと

1. **Long / Double の自然順ソートは `Arrays.sort` を使います。** 5,000,000 要素の Long で `Arrays.sort` は基数ソートの 2 倍以上速く、採用条件を満たしません。ローカル(arm64)の結果とは逆でした。
2. **コールドスタートの影響が大きいです。** 200,000 要素の 1 回目は最小値の約 10 倍かかります。プロセッサが 1 個なので、JIT コンパイルと本体が同じ CPU を取り合います。1 回実行の時間を重視して計測します。
3. **使えるヒープは約 922 MiB です。** 問題のメモリ制限が大きくても、`-Xmx` がないので増えません(物理メモリが同じ場合)。
4. **配列の拡張は `maxMemory()` よりかなり手前で失敗します。** 新旧の配列が同時に必要で、1 個の配列も約 637 MiB までです。
5. **並列化の効果はありません。** Serial GC とプロセッサ 1 個の環境です。
