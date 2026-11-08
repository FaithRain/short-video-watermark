# DESIGN.md — 短视频去水印 · Liquid Glass

## 视觉世界
浅色液态玻璃：暖白到冷蓝的雾面底，漂浮的半透明玻璃面板，
高光描边 + 柔和投影 + 低饱和彩色光斑。参考 Apple 的留白与字重纪律，
但采用 iOS 26 Liquid Glass 的材质语言，而不是 Apple 营销页的纯白画廊。

## Tokens
colors:
  canvas-top: "#F7F9FD"
  canvas-mid: "#EFF4FB"
  canvas-bottom: "#FBF6F1"
  glass-tint: "rgba(255,255,255,0.55)"
  glass-tint-strong: "rgba(255,255,255,0.72)"
  glass-border-top: "rgba(255,255,255,0.95)"
  glass-border-bottom: "rgba(255,255,255,0.25)"
  ink: "#0E1116"
  ink-muted: "#5B6472"
  accent: "#0A6CFF"
  accent-2: "#7C5CFF"
  aura-blue: "rgba(143,200,255,0.40)"
  aura-purple: "rgba(217,199,255,0.38)"
  aura-peach: "rgba(255,217,184,0.34)"
  success: "#18A058"
  danger: "#D93F3F"

typography:
  hero: 34sp / 700 / letterSpacing -0.6sp
  title: 22sp / 650 / -0.3sp
  card-title: 16sp / 600
  body: 14sp / 400 / lineHeight 21sp
  caption: 12sp / 400
  button: 16sp / 600

spacing: 4 / 8 / 12 / 16 / 20 / 24 / 32
radius: card 24dp, tile 20dp, pill 999dp, sheet 28dp
shadow: 0 18dp 40dp rgba(51,80,122,0.10)

## 规则
- 玻璃面板一律：半透明白 + 1dp 渐变高光边 + 大圆角 + 柔和外阴影；不做纯白实心卡片。
- 背景永远是雾面渐变 + 2–3 个缓慢移动的低饱和光斑；光斑不做高对比霓虹。
- 主色只用于主按钮、选中态和关键词，不大面积铺色。
- 标题用紧字距、大字号；正文克制，行高 ≥1.5。
- 图标放在 40dp 圆角玻璃砖里，不用裸图标做视觉主体。
- 动效：进入 fade+8dp 上移；按压缩放 0.97；光斑 18s 往复，不抢注意力。
- 禁止：纯黑背景、紫色渐变按钮、发光文字、拟物纹理、重投影、密集边框。

## 布局
- 首页：大标题 → 玻璃 hero → 2 列玻璃功能砖（6 个）→ 底部悬浮玻璃导航。
- 解析页：玻璃输入框 → 蓝色玻璃主按钮 → 结果玻璃卡（封面/播放器/文案切换）。
- 我的：玻璃资料卡 + 玻璃列表 + 赞赏入口；风格与首页一致。
- 赞赏页：顶部说明，支付宝/微信分段控件，二维码玻璃卡，长按识别 + 保存提示。

## 语气
短句、直给、不营销腔。"粘贴链接就能解析" 优于 "一站式智能解析解决方案"。
