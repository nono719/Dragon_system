<template>
  <div class="identicon" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :viewBox="`0 0 ${grid} ${grid}`" preserveAspectRatio="xMidYMid meet" :width="size" :height="size">
      <rect width="100%" height="100%" :fill="bg" />
      <rect
        v-for="(c, idx) in cells"
        :key="idx"
        :x="c.x"
        :y="c.y"
        width="1"
        height="1"
        :fill="c.fill"
      />
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 任意字符串（一般是钱包地址）。 */
  seed: { type: String, required: true },
  /** 视口像素 */
  size: { type: Number, default: 64 },
  /** 网格大小（5x5 左右镜像 = 偶数边格，但 5 是经典选择） */
  grid: { type: Number, default: 5 }
})

/** 把 seed 转成稳定的 32-bit 整数 hash（djb2 变体） */
function hashSeed(s) {
  let h = 5381
  for (let i = 0; i < s.length; i++) {
    h = ((h << 5) + h) ^ s.charCodeAt(i)
  }
  return h >>> 0
}

const cells = computed(() => {
  const h = hashSeed((props.seed || '').toLowerCase())
  const hue = h % 360
  const fg = `hsl(${hue}, 70%, 50%)`
  const fgAccent = `hsl(${(hue + 30) % 360}, 70%, 60%)`

  const g = props.grid
  const half = Math.ceil(g / 2)
  const arr = []
  // 用 hash 的不同位段决定每格是否填色（左右镜像）
  for (let y = 0; y < g; y++) {
    for (let x = 0; x < half; x++) {
      const bit = (h >> ((y * half + x) % 31)) & 1
      const altBit = (h >> ((y * half + x + 7) % 31)) & 1
      if (bit) {
        const color = altBit ? fgAccent : fg
        arr.push({ x, y, fill: color })
        // 镜像
        const mirrorX = g - 1 - x
        if (mirrorX !== x) arr.push({ x: mirrorX, y, fill: color })
      }
    }
  }
  return arr
})

const bg = computed(() => {
  const h = hashSeed((props.seed || '').toLowerCase())
  const hue = (h + 180) % 360
  return `hsl(${hue}, 30%, 96%)`
})
</script>

<style scoped>
.identicon {
  border-radius: 50%;
  overflow: hidden;
  display: inline-block;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.4);
}
.identicon svg { display: block; }
</style>
