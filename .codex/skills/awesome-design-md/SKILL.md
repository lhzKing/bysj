---
name: awesome-design-md
description: Use when the user wants to pick or apply a DESIGN.md reference from the VoltAgent awesome-design-md catalog, emulate the UI style of a known product or brand, compare design directions like Claude/Linear/Vercel/Stripe/Notion, or fetch a getdesign.md DESIGN.md via jshook for frontend work.
---

# Awesome Design MD

## Overview

This skill turns vague requests like “做成 Claude 风格” or “给我一个 Linear / Vercel 那种前端设计感” into a repeatable workflow.

The `awesome-design-md` GitHub repo is mainly an index. The full design-system text lives on `getdesign.md`, so when you need the actual DESIGN.md content you should fetch it on demand with **jshook**.

## Use this skill when

- The user wants a UI inspired by a specific product, brand, or website
- The user asks for `DESIGN.md`
- The user wants design direction options before implementation
- The user wants a local `DESIGN.md` saved into the project
- The user describes a mood instead of a brand, such as “warm editorial”, “dark developer tool”, or “clean fintech”

## Workflow

1. Read `references/catalog.md` to identify the best matching slug or shortlist 2-4 candidates.
2. If remote access is needed and the user has not provided `jshook`, ask for it first.
3. Open `https://getdesign.md/<slug>/design-md` with jshook.
4. Switch the page to the **DESIGN.md** tab and extract the article text.
5. Distill only the parts relevant to the task: color roles, typography, spacing, surfaces, component styling, layout rhythm, and interaction cues.
6. Apply the style to the user’s UI task.
7. Only write a local `DESIGN.md` file when the user asks for it or when the project workflow clearly benefits from keeping it in-repo.

## Fetching the full DESIGN.md with jshook

Use this URL pattern:

- `https://getdesign.md/<slug>/design-md`

The page often opens on a preview-oriented view first. A reliable jshook extraction flow is:

1. Navigate to the page
2. Run `page_evaluate` with:

```js
(async () => {
  const btn = [...document.querySelectorAll('button')]
    .find(b => (b.textContent || '').trim() === 'DESIGN.md');
  if (btn) btn.click();
  await new Promise(r => setTimeout(r, 500));
  const article = [...document.querySelectorAll('article')]
    .sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0];
  return article?.innerText || document.querySelector('main')?.innerText || '';
})()
```

If the extracted text is long, summarize it instead of pasting the whole document unless the user explicitly asks for the full text.

## Selection heuristics

Match the **product type first**, then the **visual mood**.

### Good defaults by product type

- SaaS / admin / docs: `linear.app`, `vercel`, `mintlify`, `notion`
- AI / chat / model product: `claude`, `cohere`, `opencode.ai`, `voltagent`
- Consumer polished landing pages: `apple`, `airbnb`, `spotify`
- Fintech: `stripe`, `revolut`, `coinbase`, `wise`
- Dark developer tools: `cursor`, `warp`, `expo`, `raycast`

### Good defaults by adjective

- Warm / editorial: `claude`, `notion`
- Minimal / precise / monochrome: `linear.app`, `vercel`, `apple`
- Playful / colorful: `figma`, `airtable`, `miro`
- Cinematic / dark / premium: `runwayml`, `elevenlabs`, `shopify`

## Output rules

- Tell the user which reference you picked and why
- Separate “reference traits” from “task-specific implementation”
- Don’t dump the full DESIGN.md unless asked
- If multiple styles are plausible, propose a short ranked shortlist first

## References

- `references/catalog.md` — available brands, slugs, and one-line style summaries
