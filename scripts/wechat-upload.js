const fs = require('fs');
const path = require('path');
const ci = require('miniprogram-ci');

async function main() {
  const appid = process.env.WECHAT_APPID;
  const privateKey = process.env.WECHAT_PRIVATE_KEY;
  const robot = Number(process.env.WECHAT_CI_ROBOT || 1);
  const version = process.env.WECHAT_CI_VERSION || 'dev';

  if (!appid || !privateKey) {
    throw new Error('Missing WECHAT_APPID or WECHAT_PRIVATE_KEY');
  }

  const keyPath = path.resolve(__dirname, '../skin-front/.wechat-private.key');
  fs.writeFileSync(keyPath, privateKey, 'utf8');

  const projectPath = path.resolve(__dirname, '../skin-front/dist/build/mp-weixin');
  const project = new ci.Project({
    appid,
    type: 'miniProgram',
    projectPath,
    privateKeyPath: keyPath,
    ignores: ['node_modules/**/*']
  });

  await ci.upload({
    project,
    version,
    desc: `auto upload from github actions ${version}`,
    robot
  });

  fs.unlinkSync(keyPath);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
