import '@ant-design/v5-patch-for-react-19'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import './index.css'
import './styles/admin-soft-brutalism.css'
import './styles/home-theme.css'
import App from './App.jsx'
import AppErrorBoundary from './components/AppErrorBoundary/AppErrorBoundary.jsx'

// 确保 window.dayjs 存在，Ant Design 内部可能依赖它
window.dayjs = dayjs

// 与学生端首页共享同一套暖灰背景、低饱和辅助色与轻量卡片规格。
const appTheme = {
  token: {
    colorPrimary: '#23262B',
    colorInfo: '#5C8CB4',
    colorSuccess: '#6F9463',
    colorWarning: '#A8822B',
    colorError: '#B4707F',
    colorLink: '#5C8CB4',
    colorLinkHover: '#23262B',
    colorBgLayout: '#F7F5F1',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    colorText: '#23262B',
    colorTextSecondary: '#5A6069',
    colorTextTertiary: '#8A9099',
    colorBorder: '#DED7CB',
    colorBorderSecondary: '#EAE4DA',
    colorSplit: '#EAE4DA',
    borderRadius: 12,
    borderRadiusLG: 20,
    borderRadiusSM: 10,
    boxShadow: '0 1px 2px rgba(35,38,43,.04), 0 4px 12px rgba(35,38,43,.04)',
    boxShadowSecondary: '0 2px 4px rgba(35,38,43,.04), 0 12px 28px rgba(35,38,43,.06)',
    boxShadowTertiary: '0 1px 2px rgba(35,38,43,.04)',
    fontFamily: "Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif",
    controlHeight: 42,
  },
  components: {
    Button: {
      fontWeight: 600,
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
      defaultBg: '#FFFFFF',
      defaultBorderColor: '#DED7CB',
    },
    Card: {
      headerBg: 'transparent',
      colorBorderSecondary: '#EAE4DA',
    },
    Table: {
      headerBg: '#FBF9F5',
      headerColor: '#5A6069',
      headerSplitColor: '#EAE4DA',
      rowHoverBg: '#EFF4F9',
      borderColor: '#EAE4DA',
    },
    Modal: {
      contentBg: '#FFFFFF',
      headerBg: '#FFFFFF',
    },
    Tag: {
      defaultBg: '#FBF9F5',
      defaultColor: '#5A6069',
    },
    Tabs: {
      itemSelectedColor: '#23262B',
      inkBarColor: '#5C8CB4',
    },
    Pagination: {
      itemActiveBg: '#DCE8F3',
    },
  },
}

createRoot(document.getElementById('root')).render(
  <ConfigProvider locale={zhCN} theme={appTheme}>
    <AppErrorBoundary>
      <App />
    </AppErrorBoundary>
  </ConfigProvider>,
)
