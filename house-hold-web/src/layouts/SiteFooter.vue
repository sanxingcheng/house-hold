<template>
  <footer class="site-footer" aria-label="网站备案信息">
    <!-- 备案号需要在全站页面稳定展示，并跳转到工信部备案系统。 -->
    <a
      class="icp-link"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noopener noreferrer"
    >
      苏ICP备2026025887号-1
    </a>
  </footer>
</template>

<style scoped>
.site-footer {
  width: 100%;
  padding: 16px 20px 20px;
  text-align: center;
  color: var(--site-footer-color, #909399);
  font-size: 0.85rem;
  line-height: 1.5;
}

.icp-link {
  color: inherit;
  text-decoration: none;
  word-break: keep-all;
}

.icp-link:hover {
  color: var(--site-footer-hover-color, var(--el-color-primary));
}

@media screen and (max-width: 768px) {
  .site-footer {
    padding: 12px 12px 16px;
    font-size: 0.78rem;
  }
}
</style>
