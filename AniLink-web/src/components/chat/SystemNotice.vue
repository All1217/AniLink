<template>
  <section class="system">
    <el-empty description="后端暂未提供系统通知接口" :image-size="96">
      <p class="system__desc">
        现有后端只有私信（`/main/chat/**`）相关接口，没有系统通知的查询/已读接口；
        为避免展示假数据，这里保持空状态。接口就绪后在此接入通知列表即可。
      </p>
    </el-empty>
  </section>
</template>

<script setup lang="ts">
// 系统通知：无后端接口，按 D3 同样的原则只做空状态说明
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.system {
  padding: 32px 20px;
  background: #fff;
  border-radius: var(--radius-lg);

  &__desc {
    max-width: 520px;
    margin: 0 auto;
    font-size: t.$fs-xs;
    line-height: 20px;
    color: var(--text-3);
    text-align: center;
  }
}
</style>
