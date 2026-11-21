<template>
  <section class="article">
    <el-empty description="后端暂未提供稿件列表接口" :image-size="96">
      <p class="article__desc">
        现有后端只有投稿提交（`POST /main/file/submit`）与审核状态字段，没有「我的稿件」查询接口；
        为避免展示假数据，这里保持空状态。接口就绪后在此接入分页列表即可。
      </p>
    </el-empty>
  </section>
</template>

<script setup lang="ts">
// 稿件管理：D3 决策——无接口，显示空状态说明，不造假数据
</script>

<style scoped lang="scss">
@use '@/styles/tokens' as t;

.article {
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
