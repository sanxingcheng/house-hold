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
    <span class="sep">|</span>
    <a
      class="gongan-link"
      href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=32010502011436"
      target="_blank"
      rel="noopener noreferrer"
    >
      <img class="gongan-icon" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAGbSURBVDhPnZO7SgNBFIb3fZW8gIWNpU9gYyGkURB8ALVSK0WxUCyjjY0giAqKhUgKIRjvG7mvrP/N7G7WTXSHPjizZ86c780s7P8Lu7u7k2q1epvNZq+LxeKt1+s9tNvt666Pj4/54+OjPxgMPt1u91EGl8v1IYQ4SyQSJ7Va7TaVSj01Go17r9d7C4PBYDQYDF5SqdR5PB6/jMViLyJwCIVC33C73c/lcnmrVqtdwcnJyesG4ziOFIvFbjAYfAO3cS6ECEUiEUnTNEVRNAkGk2q1+iGE+MlkMh2CIvRN0zRaWlr6g0Kh8AlhMZfLtQWF5eVlet/f36dyuUw0TUtfXV2R4zhUq9XI6XTS6uoqGYZButfr9X10oNNqtQYQ7Gxvb3+L6+vrZDgckqZpluFgMFgsFgfy+fzLysrKC0vHcZ7h9X6/n3w+n1gsFl/QZeXr62uaTqcjCCHfMBgMvm5vb98PDw+f5ubmfs3Pzz8Hg8FHBAr0eDx/C3w0nU7f7+/vv0cikddisZiHAnOxWPx0OBzfQAF6Pp//hgX8BPE/pZZl/QRY34/XxZLGAAAAAElFTkSuQmCC" alt="公安备案图标" />
      苏公安网备32010502011436号
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

.sep {
  margin: 0 6px;
  color: inherit;
  opacity: 0.4;
}

.icp-link,
.gongan-link {
  color: inherit;
  text-decoration: none;
  word-break: keep-all;
}

.icp-link:hover,
.gongan-link:hover {
  color: var(--site-footer-hover-color, var(--el-color-primary));
}

.gongan-icon {
  width: 14px;
  height: 14px;
  vertical-align: -2px;
  margin-right: 2px;
}

@media screen and (max-width: 768px) {
  .site-footer {
    padding: 12px 12px 16px;
    font-size: 0.78rem;
  }
}
</style>
