#!/usr/bin/env tsx
/**
 * notify-deploy — posts a deployment notification to Discord via webhook.
 *
 * Usage:
 *   notify-deploy --version-code <code> --version-name <name>
 *
 * Requires env: DISCORD_RHOSYS_CI_CD_CHANNEL_WEBHOOK
 */

import { PACKAGE_NAME } from './deploy-play-store';

export function buildTestingUrl(packageName: string, versionCode: string): string {
  return `https://play.google.com/apps/test/${packageName}/${versionCode}`;
}

export interface NotifyDeps {
  postToDiscord: (webhookUrl: string, body: object) => Promise<void>;
  print: (msg: string) => void;
}

export async function notifyDeploy(
  packageName: string,
  versionCode: string,
  versionName: string,
  webhookUrl: string,
  deps: NotifyDeps,
): Promise<void> {
  const testingUrl = buildTestingUrl(packageName, versionCode);

  const embed = {
    title: `📱 ${packageName} v${versionName} (${versionCode})`,
    description: `Published to Play Store Internal Testing track.\n\n**[Install / Update](${testingUrl})**`,
    color: 0x34a853,
    timestamp: new Date().toISOString(),
  };

  deps.print(`Posting deploy notification to Discord...`);
  await deps.postToDiscord(webhookUrl, { embeds: [embed] });
  deps.print(`✓ Notification sent.`);
}

function parseArgs(args: string[]): { versionCode: string; versionName: string } {
  let versionCode = '';
  let versionName = '';

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--version-code' && args[i + 1]) versionCode = args[++i]!;
    else if (args[i] === '--version-name' && args[i + 1]) versionName = args[++i]!;
  }

  if (!versionCode || !versionName) {
    process.stderr.write('Usage: notify-deploy --version-code <code> --version-name <name>\n');
    process.exit(1);
  }

  return { versionCode, versionName };
}

async function main(): Promise<void> {
  const { versionCode, versionName } = parseArgs(process.argv.slice(2));
  const webhookUrl = process.env.DISCORD_RHOSYS_CI_CD_CHANNEL_WEBHOOK;

  if (!webhookUrl) {
    process.stderr.write('Warning: DISCORD_RHOSYS_CI_CD_CHANNEL_WEBHOOK not set — skipping notification.\n');
    return;
  }

  await notifyDeploy(PACKAGE_NAME, versionCode, versionName, webhookUrl, {
    postToDiscord: async (url, body) => {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        throw new Error(`Discord webhook failed: ${res.status} ${await res.text()}`);
      }
    },
    print: (msg) => process.stderr.write(msg + '\n'),
  });
}

if (require.main === module) {
  main().catch((err: unknown) => {
    process.stderr.write(`Warning: Failed to send notification: ${err instanceof Error ? err.message : err}\n`);
    // Non-fatal — don't fail the deploy job over a notification
    process.exit(0);
  });
}
