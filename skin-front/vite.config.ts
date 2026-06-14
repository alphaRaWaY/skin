import { rmSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const pruneMiniProgramAssets = () => {
  const pruneOutput = () => {
    if (process.env.UNI_PLATFORM !== 'mp-weixin') return

    const buildOutput = process.env.UNI_OUTPUT_DIR || 'dist/build/mp-weixin'
    const outputDir = resolve(__dirname, buildOutput, 'static')
    for (const directory of ['design', 'images', 'legacy', 'tabs']) {
      rmSync(resolve(outputDir, directory), { recursive: true, force: true })
    }
    rmSync(resolve(outputDir, 'logo.png'), { force: true })
    rmSync(resolve(outputDir, 'tabs-doctor/start.png'), { force: true })
  }

  return {
    name: 'prune-mini-program-assets',
    apply: 'build' as const,
    // writeBundle runs after every development-watch rebuild.
    writeBundle: pruneOutput,
    closeBundle: pruneOutput,
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        silenceDeprecations: ['legacy-js-api', 'global-builtin', 'import'],
      },
    },
  },
  build: {
    // WeChat device debugging does not need source maps in the upload package.
    sourcemap:
      process.env.NODE_ENV === 'development' &&
      process.env.UNI_PLATFORM !== 'mp-weixin',
  },
  plugins: [uni(), pruneMiniProgramAssets()],
})
