<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en" data-theme="auto">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <title>${runName} - PulseReport</title>
    <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,%3Csvg xmlns='http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg' viewBox='0 0 256 256'%3E%3Cstyle%3E.i%7Bfill:%23212529%7D.s%7Bfill:%230F8B8D%7D%40media (prefers-color-scheme:dark)%7B.i%7Bfill:%23F8F9FA%7D.s%7Bfill:%2358B7B8%7D%7D%3C/style%3E%3Cg class='i'%3E%3Ccircle cx='32' cy='150' r='18'/%3E%3Crect x='112' y='132' width='36' height='108' rx='18'/%3E%3Crect x='161' y='92' width='36' height='76' rx='18'/%3E%3Ccircle cx='228' cy='150' r='18'/%3E%3C/g%3E%3Cpath class='s' d='M63 150 L81 16 L99 150 A18 18 0 0 1 63 150 Z'/%3E%3C/svg%3E">
    <style>${fontFaces?no_esc}</style>
    <#noparse>
    <script>
        try {
            var t = localStorage.getItem('pulse-report-theme');
            if (t === 'light' || t === 'dark') document.documentElement.dataset.theme = t;
        } catch (e) {}
    </script>
    <style>
        *,
        *::before,
        *::after {
            box-sizing: border-box;
        }

        [hidden] {
            display: none !important;
        }

        :root {
            --font-sans: 'Geist', 'Geist Variable', ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
            --font-mono: 'Geist Mono', ui-monospace, 'SF Mono', SFMono-Regular, 'JetBrains Mono', Menlo, Consolas, monospace;
            --radius: 6px;
            --surface: #f6f7f8;
            --raised: #fcfcfd;
            --sunken: #eceef0;
            --border: #dde1e5;
            --border-strong: #c5cbd1;
            --shot-edge: #c5cbd1;
            --ink: #212529;
            --ink-muted: #59616a;
            --accent: #0f8b8d;
            --accent-ink: #0a6c6e;
            --accent-bg: #e2f1f1;
            --pass: #2b7a3d;
            --pass-bg: #e5f2e8;
            --fail: #c0362f;
            --fail-bg: #fbe9e7;
            --skip: #8a5f00;
            --skip-bg: #f7eed8;
            --bar-skip: #f6c73c;
            --chip-all-bg: var(--ink);
            --chip-all-fg: #fff;
            color-scheme: light;
        }

        @media screen {
            :root[data-theme='dark'] {
                --surface: #121416;
                --raised: #181b1e;
                --sunken: #0d0f11;
                --border: #2a2f34;
                --border-strong: #3a4046;
                --shot-edge: #646d76;
                --ink: #e6e8ea;
                --ink-muted: #9aa2a9;
                --accent: #2fb3b5;
                --accent-ink: #5cc8ca;
                --accent-bg: #11302f;
                --pass: #5bbf73;
                --pass-bg: #142a1b;
                --fail: #f07171;
                --fail-bg: #311617;
                --skip: #e0ad48;
                --skip-bg: #2c2312;
                --bar-skip: #e4cf6e;
                --chip-all-bg: var(--border-strong);
                --chip-all-fg: var(--ink);
                color-scheme: dark;
            }
        }

        @media screen and (prefers-color-scheme: dark) {
            :root[data-theme='auto'] {
                --surface: #121416;
                --raised: #181b1e;
                --sunken: #0d0f11;
                --border: #2a2f34;
                --border-strong: #3a4046;
                --shot-edge: #646d76;
                --ink: #e6e8ea;
                --ink-muted: #9aa2a9;
                --accent: #2fb3b5;
                --accent-ink: #5cc8ca;
                --accent-bg: #11302f;
                --pass: #5bbf73;
                --pass-bg: #142a1b;
                --fail: #f07171;
                --fail-bg: #311617;
                --skip: #e0ad48;
                --skip-bg: #2c2312;
                --bar-skip: #e4cf6e;
                --chip-all-bg: var(--border-strong);
                --chip-all-fg: var(--ink);
                color-scheme: dark;
            }
        }

        html,
        body {
            margin: 0;
        }

        body {
            font: 13px/1.5 var(--font-sans);
            color: var(--ink);
            background: var(--surface);
            -webkit-font-smoothing: antialiased;
        }

        button {
            font: inherit;
            color: inherit;
        }

        code,
        pre,
        kbd,
        .mono {
            font-family: var(--font-mono);
        }

        .muted {
            color: var(--ink-muted);
        }

        :focus-visible {
            outline: 2px solid var(--accent-ink);
            outline-offset: 2px;
            border-radius: var(--radius);
        }

        .sr-only {
            position: absolute;
            width: 1px;
            height: 1px;
            margin: -1px;
            padding: 0;
            overflow: hidden;
            clip: rect(0 0 0 0);
            white-space: nowrap;
            border: 0;
        }

        .glyph {
            display: inline-block;
            width: 7px;
            height: 7px;
            flex: none;
            border-radius: 50%;
            background: currentColor;
        }

        .glyph.s-skip {
            color: var(--bar-skip);
        }

        .ic {
            width: 16px;
            height: 16px;
            flex: none;
            fill: none;
            stroke: currentColor;
            stroke-width: 1.75;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        .s-pass {
            color: var(--pass);
        }

        .s-fail {
            color: var(--fail);
        }

        .s-skip {
            color: var(--skip);
        }

        .app {
            display: grid;
            grid-template-rows: auto auto minmax(0, 1fr);
            height: 100dvh;
        }

        /* Top bar */
        .topbar {
            display: flex;
            align-items: center;
            gap: 20px;
            height: 56px;
            padding: 0 20px;
            background: var(--raised);
            border-bottom: 1px solid var(--border);
            min-width: 0;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
            min-width: 0;
            flex: 0 1 auto;
        }

        .brand-mark {
            width: 22px;
            height: 22px;
            flex: none;
        }

        .bm-ink {
            fill: var(--ink);
        }

        .bm-accent {
            fill: var(--accent);
        }

        .run-name {
            margin: 0;
            font-size: 14px;
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .run-meta {
            position: relative;
            display: flex;
            gap: 18px;
            flex: 1 1 auto;
            min-width: 0;
            overflow: hidden;
            white-space: nowrap;
            font-size: 12px;
            color: var(--ink-muted);
        }

        .run-meta .meta {
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .run-meta .ic {
            width: 14px;
            height: 14px;
        }

        .run-meta .v {
            font-family: var(--font-mono);
            color: var(--ink);
        }

        .actions {
            display: flex;
            gap: 8px;
            flex: none;
            margin-left: auto;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            height: 30px;
            padding: 0 12px;
            border: 1px solid var(--border-strong);
            border-radius: var(--radius);
            background: var(--raised);
            font-size: 12px;
            font-weight: 500;
            white-space: nowrap;
            cursor: pointer;
        }

        .btn:hover {
            background: var(--sunken);
        }

        .btn:active {
            transform: translateY(1px);
        }

        .icon-btn {
            width: 30px;
            padding: 0;
            justify-content: center;
        }

        .ghost {
            display: inline-grid;
            place-items: center;
            width: 26px;
            height: 26px;
            padding: 0;
            border: 0;
            border-radius: var(--radius);
            background: none;
            color: var(--ink-muted);
            cursor: pointer;
        }

        .ghost:hover {
            background: var(--sunken);
            color: var(--ink);
        }

        .ghost.ok {
            color: var(--pass);
        }

        .actions .ghost {
            width: 32px;
            height: 32px;
        }

        .actions .ghost .ic {
            width: 20px;
            height: 20px;
        }

        .tip {
            position: fixed;
            left: 0;
            top: 0;
            z-index: 10;
            max-width: 320px;
            padding: 7px 10px;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
            color: var(--ink);
            font: 12px/1.45 var(--font-sans);
            overflow-wrap: anywhere;
            box-shadow:
                0 8px 24px -6px rgb(13 15 17 / 0.22),
                0 2px 6px -2px rgb(13 15 17 / 0.12);
            pointer-events: none;
            opacity: 0;
            visibility: hidden;
        }

        .tip.on {
            opacity: 1;
            visibility: visible;
        }

        .tip strong {
            display: block;
            font-weight: 600;
        }

        .tip .tip-sub {
            display: block;
            margin-top: 1px;
            font-family: var(--font-mono);
            font-size: 11.5px;
            color: var(--ink-muted);
            white-space: pre-line;
        }

        /* Verdict band */
        .verdict {
            display: grid;
            grid-template-columns: minmax(200px, max-content) minmax(0, 1fr);
            gap: 48px;
            align-items: end;
            padding: 24px 20px 18px;
            border-bottom: 1px solid var(--border);
        }

        .verdict-title {
            margin: 0;
            font-size: 40px;
            line-height: 1.05;
            font-weight: 600;
            letter-spacing: -0.02em;
            font-variant-numeric: tabular-nums;
        }

        .verdict-title.is-fail {
            color: var(--fail);
        }

        .verdict-sub {
            margin: 6px 0 0;
            font-size: 15px;
            color: var(--ink-muted);
        }

        .breakdown {
            display: flex;
            gap: 18px;
            margin-top: 12px;
            font-size: 12px;
            color: var(--ink-muted);
        }

        .breakdown b {
            margin: 0 4px 0 6px;
            font-family: var(--font-mono);
            font-weight: 600;
            color: var(--ink);
        }

        .pulse {
            min-width: 0;
        }

        .pulse-track {
            display: flex;
            align-items: flex-end;
            justify-content: space-between;
            gap: var(--bar-gap, 3px);
            height: 68px;
            padding-bottom: 12px;
            overflow-x: auto;
            scrollbar-width: thin;
        }

        .pulse-track.sparse {
            justify-content: flex-start;
            gap: 6px;
        }

        .bar {
            position: relative;
            flex: 1 1 0;
            min-width: 2px;
            max-width: 14px;
            border-radius: 999px;
            background: var(--pass);
            opacity: 0.4;
            cursor: pointer;
        }

        .bar[data-s='fail'] {
            background: var(--fail);
        }

        .bar[data-s='skip'] {
            background: var(--bar-skip);
        }

        .bar:hover {
            opacity: 0.7;
        }

        .bar[aria-current='true'] {
            opacity: 1;
        }

        .bar[aria-current='true']::after {
            content: '';
            position: absolute;
            left: 50%;
            bottom: -10px;
            width: 5px;
            height: 5px;
            margin-left: -2.5px;
            border-radius: 50%;
            background: var(--accent);
        }

        .pulse-axis {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 16px;
            margin-top: 4px;
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .legend,
        .axis-label {
            display: flex;
            gap: 14px;
            font-family: var(--font-sans);
        }

        .sw {
            display: inline-block;
            width: 5px;
            height: 11px;
            margin-right: 6px;
            border-radius: 999px;
            vertical-align: -1px;
        }

        /* Panes */
        .panes {
            display: grid;
            grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
            min-height: 0;
        }

        .app.is-empty .panes {
            grid-template-columns: 1fr;
        }

        .app.is-empty .list-pane {
            display: none;
        }

        .list-pane {
            display: flex;
            flex-direction: column;
            min-height: 0;
            background: var(--raised);
            border-right: 1px solid var(--border);
        }

        .list-controls {
            padding: 14px 14px 12px;
            border-bottom: 1px solid var(--border);
        }

        .search-wrap {
            position: relative;
        }

        .search-wrap .ic {
            position: absolute;
            left: 10px;
            top: 50%;
            width: 14px;
            height: 14px;
            transform: translateY(-50%);
            color: var(--ink-muted);
            pointer-events: none;
        }

        .search-wrap input {
            width: 100%;
            height: 32px;
            padding: 0 10px 0 32px;
            border: 1px solid var(--border-strong);
            border-radius: var(--radius);
            background: var(--surface);
            color: var(--ink);
            font: inherit;
        }

        .search-wrap input:focus {
            outline: none;
            border-color: var(--accent);
        }

        .search-wrap input::placeholder {
            color: var(--ink-muted);
        }

        kbd {
            display: inline-block;
            min-width: 18px;
            padding: 0 5px;
            border: 1px solid var(--border-strong);
            border-bottom-width: 2px;
            border-radius: var(--radius);
            background: var(--raised);
            font-size: 11px;
            line-height: 16px;
            text-align: center;
            color: var(--ink-muted);
        }

        .chips {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            min-width: 0;
            margin: 10px 0 0;
            padding: 0;
            border: 0;
        }

        .chip {
            --chip-fg: var(--ink);
            --chip-bg: var(--sunken);
            --chip-line: var(--ink-muted);
            display: inline-flex;
            align-items: center;
            gap: 6px;
            height: 26px;
            padding: 0 10px;
            border: 1px solid var(--border-strong);
            border-radius: 999px;
            background: transparent;
            font-size: 12px;
            cursor: pointer;
        }

        .chip[data-status='FAILED'] {
            --chip-fg: var(--fail);
            --chip-bg: var(--fail-bg);
            --chip-line: var(--fail);
        }

        .chip[data-status='SKIPPED'] {
            --chip-fg: var(--bar-skip);
            --chip-bg: var(--skip-bg);
            --chip-line: var(--bar-skip);
        }

        .chip[data-status='PASSED'] {
            --chip-fg: var(--pass);
            --chip-bg: var(--pass-bg);
            --chip-line: var(--pass);
        }

        .chip .n {
            font-family: var(--font-mono);
            font-size: 11px;
            color: var(--ink-muted);
        }

        .chip:hover:not(:disabled):not([aria-pressed='true']) {
            border-color: var(--chip-line);
            color: var(--chip-fg);
        }

        .chip[aria-pressed='true'] {
            background: var(--chip-fg);
            border-color: var(--chip-line);
            color: white;
        }

        .chip[data-status='all'][aria-pressed='true'] {
            background: var(--chip-all-bg);
            border-color: var(--chip-all-bg);
            color: var(--chip-all-fg);
        }

        .chip:hover:not(:disabled) .n,
        .chip[aria-pressed='true'] .n {
            color: inherit;
            opacity: 0.75;
        }

        .chip:disabled {
            opacity: 0.45;
            cursor: default;
        }

        /* position:relative keeps absolutely positioned .sr-only labels inside the scroller instead of stretching the page */
        .tree {
            position: relative;
            flex: 1;
            overflow: auto;
            padding: 6px 0 12px;
        }

        .tree-empty {
            padding: 16px 14px;
            color: var(--ink-muted);
        }

        .tree-empty p {
            margin: 0 0 6px;
        }

        .dz > summary {
            display: flex;
            align-items: center;
            gap: 8px;
            list-style: none;
            cursor: pointer;
        }

        .dz > summary::-webkit-details-marker {
            display: none;
        }

        .dz > summary::before {
            content: '';
            flex: none;
            width: 5px;
            height: 5px;
            margin: 0 2px;
            border-right: 1.5px solid var(--ink-muted);
            border-bottom: 1.5px solid var(--ink-muted);
            transform: rotate(-45deg);
        }

        .dz[open] > summary::before {
            transform: rotate(45deg);
        }

        .group > summary {
            padding: 8px 14px;
            font-size: 12.5px;
            font-weight: 600;
        }

        .group > summary:hover {
            background: var(--sunken);
        }

        .gname {
            flex: 1;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .gbadge {
            font-size: 11px;
            font-weight: 600;
            color: var(--fail);
        }

        .gcount {
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .items {
            list-style: none;
            margin: 0 0 4px;
            padding: 0;
        }

        .item {
            display: grid;
            grid-template-columns: 16px minmax(0, 1fr) auto;
            align-items: center;
            gap: 8px;
            width: 100%;
            padding: 5px 14px 5px 27px;
            border: 0;
            background: none;
            text-align: left;
            cursor: pointer;
        }

        .item .glyph,
        .step .glyph {
            justify-self: center;
        }

        .item:hover {
            background: var(--sunken);
        }

        .item:focus-visible {
            outline-offset: -2px;
            border-radius: 0;
        }

        .item[aria-current='true'] {
            background: var(--accent-bg);
            box-shadow: inset 2px 0 0 var(--accent);
        }

        .item .name {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .item .dur {
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .shortcuts {
            display: flex;
            flex-wrap: wrap;
            gap: 4px 14px;
            margin: 0;
            padding: 10px 14px;
            border-top: 1px solid var(--border);
            font-size: 11px;
            color: var(--ink-muted);
        }

        /* Detail */
        .detail-pane {
            position: relative;
            overflow: auto;
            min-width: 0;
        }

        .detail {
            container-type: inline-size;
            max-width: 1100px;
            padding: 22px 28px 56px;
        }

        .back {
            display: none;
        }

        .d-head h2 {
            margin: 0;
            font-size: 18px;
            line-height: 1.3;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .d-meta {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 6px 16px;
            margin-top: 8px;
            font-size: 12px;
            color: var(--ink-muted);
        }

        .pill {
            display: inline-flex;
            align-items: center;
            height: 20px;
            padding: 0 8px;
            border-radius: 999px;
            background: var(--sunken);
            font-size: 11px;
            font-weight: 500;
            color: var(--ink);
        }

        .tags {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            margin-top: 10px;
        }

        .tag {
            display: inline-flex;
            align-items: center;
            height: 20px;
            padding: 0 8px;
            border: 1px solid var(--border);
            border-radius: 999px;
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .section {
            margin-top: 28px;
        }

        .section > h3 {
            margin: 0 0 10px;
            font-size: 13px;
            font-weight: 600;
        }

        .desc {
            max-width: 72ch;
            margin: 0 0 10px;
            color: var(--ink-muted);
            white-space: pre-wrap;
        }

        .panel {
            overflow: hidden;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
        }

        .linkbtn {
            padding: 0;
            border: 0;
            background: none;
            font-size: 12px;
            font-weight: 500;
            color: var(--accent-ink);
            cursor: pointer;
        }

        .linkbtn:hover {
            text-decoration: underline;
        }

        .failure .msg {
            padding: 14px 16px;
            background: var(--fail-bg);
            border-bottom: 1px solid var(--border);
        }

        .failure.is-skip .msg {
            background: var(--skip-bg);
        }

        .exc {
            margin-bottom: 6px;
            font: 600 12px var(--font-mono);
            color: var(--fail);
        }

        .failure.is-skip .exc {
            color: var(--skip);
        }

        .msg pre {
            margin: 0;
            font: 13px/1.55 var(--font-mono);
            white-space: pre-wrap;
            overflow-wrap: anywhere;
        }

        .msg pre.clamp {
            max-height: calc(1.55em * 4);
            overflow: hidden;
            -webkit-mask-image: linear-gradient(#000 60%, transparent);
            mask-image: linear-gradient(#000 60%, transparent);
        }

        .msg .linkbtn {
            margin-top: 8px;
        }

        .trace {
            padding: 10px 0 12px;
            font: 12px/1.65 var(--font-mono);
        }

        .trace-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 16px 6px;
            font: 12px var(--font-sans);
            color: var(--ink-muted);
        }

        .frame {
            padding: 0 16px 0 32px;
            text-indent: -16px;
            white-space: pre-wrap;
            overflow-wrap: anywhere;
        }

        .frame.fw {
            color: var(--ink-muted);
        }

        .frame.cause {
            margin-top: 6px;
            font-weight: 600;
        }

        .frames-fw > summary {
            padding: 0 16px;
            font: 12px/1.9 var(--font-sans);
            color: var(--ink-muted);
        }

        .steps {
            list-style: none;
            margin: 0;
            padding: 0px 0;
        }

        .step {
            display: grid;
            grid-template-columns: 16px 46px minmax(0, 1fr) auto;
            gap: 10px;
            align-items: baseline;
            padding: 6px 16px;
        }

        .step .kw {
            font-weight: 600;
            color: var(--ink-muted);
        }

        .step .stext {
            overflow-wrap: anywhere;
        }

        .step .arg {
            font: 12px var(--font-mono);
            color: var(--accent-ink);
        }

        .step .dur {
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .step-end {
            align-self: start;
            justify-self: end;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            margin-top: -1px;
        }

        .step-end .dur {
            min-width: 6ch;
            text-align: right;
        }

        .ghost.step-shot {
            display: inline-flex;
            gap: 3px;
            width: auto;
            min-width: 22px;
            height: 22px;
            padding: 0 3px;
            font: 11px var(--font-mono);
        }

        .step-desc {
            display: block;
            margin-top: 2px;
            font-size: 12px;
            color: var(--ink-muted);
        }

        .step-extra {
            grid-column: 3/-1;
            display: grid;
            gap: 10px;
            margin: 6px 0 4px;
            min-width: 0;
        }

        .step-err {
            margin: 0;
            font: 12px/1.55 var(--font-mono);
            color: var(--fail);
            white-space: pre-wrap;
            overflow-wrap: anywhere;
        }

        .awrap > summary {
            font: 12px var(--font-mono);
            color: var(--ink-muted);
        }

        .awrap[open] > summary {
            margin-bottom: 4px;
        }

        /* Fixed layout with percentage columns keeps Expected/Actual aligned across every step of a test. */
        .dt.at {
            width: 100%;
            table-layout: fixed;
        }

        .at .c-msg {
            width: 32%;
        }

        .at .c-exp,
        .at .c-act {
            width: 26%;
        }

        .at .c-fn {
            width: 16%;
        }

        .at td {
            padding-top: 5px;
            padding-bottom: 5px;
            white-space: pre-wrap;
            overflow-wrap: anywhere;
        }

        .at .a-text {
            font: 12.5px/1.45 var(--font-sans);
            color: var(--ink);
        }

        .at .a-exp,
        .at .a-act {
            color: var(--pass);
        }

        .at .is-fail .a-act {
            color: var(--fail);
        }

        .at th:last-child,
        .at .a-fn {
            padding-right: 0;
            text-align: right;
        }

        .at .a-fn {
            overflow: hidden;
            font-size: 11px;
            color: var(--ink-muted);
            white-space: nowrap;
            text-overflow: ellipsis;
        }

        .bg {
            border-bottom: 1px solid var(--border);
        }

        .bg > summary {
            padding: 8px 16px;
            font-size: 12px;
            color: var(--ink-muted);
        }

        .xwrap > summary {
            font: 12px var(--font-mono);
            color: var(--ink-muted);
        }

        .xwrap[open] > summary {
            margin-bottom: 8px;
        }

        .xcode {
            font-weight: 600;
        }

        .xlist {
            display: grid;
            gap: 10px;
        }

        .dt-scroll {
            max-width: 100%;
            overflow-x: auto;
        }

        .dt {
            border-collapse: collapse;
            font: 12px var(--font-mono);
        }

        .dt th,
        .dt td {
            padding: 3px 20px 3px 0;
            text-align: left;
            vertical-align: top;
        }

        .dt th {
            font-weight: 500;
            color: var(--ink-muted);
            border-bottom: 1px solid var(--border);
        }

        .dt tbody tr + tr td {
            border-top: 1px solid var(--border);
        }

        .dt .dt-n {
            padding-right: 12px;
            text-align: right;
            color: var(--ink-muted);
        }

        .xchg {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            overflow: hidden;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
        }

        .xchg + .xchg {
            margin-top: 12px;
        }

        .xside {
            min-width: 0;
            padding: 12px 14px;
        }

        .xside + .xside {
            border-left: 1px solid var(--border);
        }

        .xlabel {
            margin-bottom: 4px;
            font-size: 12px;
            font-weight: 600;
            color: var(--ink-muted);
        }

        .xline {
            font: 12px var(--font-mono);
            overflow-wrap: anywhere;
        }

        .xstatus {
            font-weight: 600;
        }

        .xnone {
            margin: 0;
            color: var(--ink-muted);
        }

        .hdrs {
            margin-top: 8px;
        }

        .hdrs > summary {
            font-size: 12px;
            color: var(--ink-muted);
        }

        .hdrs dl {
            display: grid;
            grid-template-columns: max-content minmax(0, 1fr);
            gap: 2px 12px;
            margin: 6px 0 0;
            font: 11.5px var(--font-mono);
        }

        .hdrs dt {
            color: var(--ink-muted);
        }

        .hdrs dd {
            margin: 0;
            overflow-wrap: anywhere;
        }

        .body {
            max-height: 280px;
            margin: 10px 0 0;
            padding: 10px 12px;
            overflow: auto;
            border-radius: var(--radius);
            background: var(--sunken);
            font: 12px/1.55 var(--font-mono);
            white-space: pre-wrap;
            overflow-wrap: anywhere;
        }

        .xnote {
            margin: 6px 0 0;
            font-size: 11px;
            color: var(--ink-muted);
        }

        @container (max-width:720px) {
            .xchg {
                grid-template-columns: 1fr;
            }

            .xside + .xside {
                border-left: 0;
                border-top: 1px solid var(--border);
            }

            /* Narrow panes stack each assertion: message, then labelled values. */
            .dt.at,
            .at tbody,
            .at tr,
            .at td {
                display: block;
                width: auto;
            }

            .at thead,
            .at colgroup,
            .at .a-fn {
                display: none;
            }

            .at tr {
                padding: 5px 0;
            }

            .dt.at tbody tr + tr {
                border-top: 1px solid var(--border);
            }

            .dt.at tbody tr + tr td {
                border-top: 0;
            }

            .at td {
                padding: 0;
            }

            .at .a-exp,
            .at .a-act {
                display: inline-block;
                max-width: 100%;
                margin-right: 16px;
                vertical-align: top;
            }

            .at .a-exp::before,
            .at .a-act::before {
                content: attr(data-label) ' ';
                color: var(--ink-muted);
            }
        }

        .shots {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 14px;
        }

        .shot {
            min-width: 0;
            padding: 0;
            border: 0;
            background: none;
            text-align: left;
            cursor: zoom-in;
        }

        .shot img {
            display: block;
            width: 100%;
            aspect-ratio: 16/10;
            object-fit: contain;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--sunken);
        }

        .shot:hover img {
            border-color: var(--border-strong);
        }

        .shot .cap,
        .video .cap {
            display: block;
            margin-top: 6px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font: 11.5px var(--font-mono);
            color: var(--ink-muted);
        }

        .shot-missing {
            display: grid;
            place-items: center;
            aspect-ratio: 16/10;
            border: 1px dashed var(--border-strong);
            border-radius: var(--radius);
            color: var(--ink-muted);
            font-size: 12px;
        }

        .carousel {
            display: grid;
            gap: 10px;
            max-width: 820px;
            -webkit-user-select: none;
            user-select: none;
        }

        .carousel:focus {
            outline: none;
        }

        .car-stage,
        .car-head {
            display: grid;
            grid-template-columns: minmax(36px, 1fr) var(--shot-w, minmax(0, 1fr)) minmax(36px, 1fr);
            align-items: center;
            gap: 10px;
        }

        .car-head .car-meta {
            grid-column: 2;
        }

        .car-thumbs {
            width: var(--shot-w, auto);
            max-width: 100%;
            margin: 0 auto;
        }

        .side-btn.prev {
            justify-self: end;
        }

        .side-btn.next {
            justify-self: start;
        }

        .car-meta .side-btn {
            width: 28px;
            height: 28px;
            margin: -4px -2px -4px 0;
        }

        .car-meta .ghost {
            flex: none;
            margin: -4px 0;
        }

        .car-frame {
            position: relative;
            grid-row: 1;
            grid-column: 2;
            overflow: hidden;
            border-radius: var(--radius);
        }

        /* Drawn on the frame, not the image, so it stays put while screenshots slide */
        .car-frame::after {
            content: '';
            position: absolute;
            inset: 0;
            border: 1px solid var(--shot-edge);
            border-radius: inherit;
            pointer-events: none;
        }

        .is-expanded .car-frame::after {
            border-color: rgb(255 255 255 / 0.22);
        }

        .car-ghost {
            position: absolute;
            inset: 0;
            pointer-events: none;
        }

        .car-view {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            padding: 0;
            border: 0;
            background: none;
            touch-action: pan-y pinch-zoom;
        }

        button.car-view {
            cursor: zoom-in;
        }

        .car-view img {
            display: block;
            max-width: 100%;
            max-height: min(460px, 60vh);
            /* Square: the frame rounds the corners, so sliding images meet without a seam */
            border-radius: 0;
            transform-origin: 0 0;
            -webkit-user-drag: none;
        }

        .carousel.is-expanded {
            max-width: none;
        }

        .is-expanded .car-view img {
            max-height: calc(92dvh - 170px);
        }

        .is-expanded .car-frame.is-zoomed img {
            cursor: grab;
        }

        .is-expanded .car-frame.is-panning img {
            cursor: grabbing;
        }

        .car-zoom {
            position: absolute;
            right: 10px;
            bottom: 10px;
            padding: 3px 8px;
            border-radius: 999px;
            background: rgb(13 15 17 / 0.65);
            color: #fff;
            font: 11.5px var(--font-mono);
            opacity: 0;
            pointer-events: none;
        }

        .car-zoom.show {
            opacity: 1;
        }

        .car-meta {
            display: flex;
            align-items: center;
            gap: 12px;
            min-width: 0;
            font-size: 12.5px;
        }

        .car-count,
        .car-file {
            flex: none;
            font: 11.5px var(--font-mono);
            color: var(--ink-muted);
        }

        .car-cap {
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .car-file {
            margin-left: auto;
        }

        .car-meta .ghost.goto {
            margin-left: -8px;
            color: var(--accent-ink);
        }

        .car-meta .ghost.goto:hover {
            background: var(--accent-bg);
        }

        .car-thumbs {
            position: relative;
            display: flex;
            gap: 8px;
            overflow-x: auto;
            padding: 2px 2px 4px;
        }

        .car-thumb {
            position: relative;
            flex: none;
            padding: 0;
            border: 0;
            background: none;
            opacity: 0.6;
            cursor: pointer;
        }

        .car-thumb img {
            display: block;
            height: 60px;
            width: auto;
            min-width: 40px;
            max-width: 240px;
            object-fit: contain;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--sunken);
        }

        .car-thumb:hover,
        .car-thumb[aria-current] {
            opacity: 1;
        }

        .car-thumb[aria-current] img {
            border-color: var(--accent);
            box-shadow: 0 0 0 1px var(--accent);
        }

        .side-btn {
            grid-row: 1;
            display: grid;
            place-items: center;
            width: 36px;
            height: 36px;
            padding: 0;
            border: 0;
            border-radius: var(--radius);
            background: none;
            color: var(--ink-muted);
            cursor: pointer;
        }

        .side-btn .ic {
            width: 24px;
            height: 24px;
            stroke-width: 2;
        }

        .side-btn:hover {
            color: var(--ink);
        }

        .side-btn:active {
            scale: 0.88;
        }

        .side-btn.prev {
            grid-column: 1;
        }

        .side-btn.next {
            grid-column: 3;
        }

        .car-live {
            display: flex;
            align-items: baseline;
            gap: 12px;
            min-width: 0;
        }

        .car-thumb.is-fail img {
            border-color: var(--fail);
            box-shadow: 0 0 0 1px var(--fail);
        }

        .car-num {
            position: absolute;
            left: 4px;
            bottom: 4px;
            min-width: 16px;
            padding: 0 4px;
            border-radius: 4px;
            background: rgb(13 15 17 / 0.7);
            color: #fff;
            font: 600 10.5px/16px var(--font-mono);
            text-align: center;
        }

        .car-thumb[aria-current] .car-num {
            background: var(--accent);
        }

        .car-thumb.is-fail .car-num {
            background: var(--fail);
        }

        .car-thumb.is-fail[aria-current] img {
            box-shadow: 0 0 0 2px var(--fail);
        }

        .shot-fail {
            flex: none;
            font-weight: 600;
            color: var(--fail);
        }

        .step.flash {
            background: var(--accent-bg);
            box-shadow: inset 3px 0 0 var(--accent);
        }

        .step-shots > summary {
            font-size: 12px;
            color: var(--ink-muted);
        }

        .step-shots[open] > summary {
            margin-bottom: 8px;
        }

        .step-shots .shots {
            grid-template-columns: repeat(auto-fill, minmax(140px, 200px));
        }

        .videos {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(min(100%, 360px), 1fr));
            gap: 14px;
        }

        .player {
            display: grid;
            overflow: hidden;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
        }

        .player:focus {
            outline: none;
        }

        .player:focus-visible {
            outline: 2px solid var(--accent-ink);
            outline-offset: 2px;
        }

        .pl-screen {
            position: relative;
            display: grid;
            place-items: center;
            min-height: 0;
            background: #0d0f11;
            cursor: pointer;
        }

        .pl-screen video {
            display: block;
            width: 100%;
            max-height: 480px;
        }

        .pl-big {
            position: absolute;
            inset: 0;
            display: grid;
            place-items: center;
            width: 52px;
            height: 52px;
            margin: auto;
            border-radius: 50%;
            background: rgb(13 15 17 / 0.62);
            box-shadow: 0 0 0 1px rgb(255 255 255 / 0.14);
            color: #fff;
            -webkit-backdrop-filter: blur(6px);
            backdrop-filter: blur(6px);
            pointer-events: none;
        }

        .pl-big .ic {
            width: 22px;
            height: 22px;
            margin-left: 2px;
        }

        .pl-screen:hover .pl-big {
            background: rgb(13 15 17 / 0.8);
        }

        .player.is-playing .pl-big {
            opacity: 0;
            scale: 0.9;
        }

        .pl-bar {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 8px;
            border-top: 1px solid var(--border);
        }

        .pl-btn {
            display: grid;
            place-items: center;
            flex: none;
            min-width: 28px;
            height: 28px;
            padding: 0 6px;
            border: 0;
            border-radius: var(--radius);
            background: none;
            color: var(--ink-muted);
            font: 11.5px var(--font-mono);
            cursor: pointer;
        }

        .pl-btn:hover {
            background: var(--sunken);
            color: var(--ink);
        }

        .pl-rate-wrap {
            position: relative;
            flex: none;
        }

        /* Wide enough for "1.5×" so the seek bar never shifts */
        .pl-rate {
            min-width: 44px;
        }

        .pl-menu {
            position: absolute;
            right: 0;
            bottom: calc(100% + 6px);
            left: 0;
            z-index: 1;
            display: grid;
            gap: 2px;
            padding: 3px;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
            box-shadow: 0 8px 24px rgb(0 0 0 / 0.18);
        }

        .pl-menu button {
            height: 30px;
            padding: 0;
            border: 0;
            border-radius: 4px;
            background: none;
            color: var(--ink-muted);
            font: 11.5px var(--font-mono);
            cursor: pointer;
        }

        .pl-menu button:hover,
        .pl-menu button:focus-visible {
            outline: none;
            background: var(--sunken);
            color: var(--ink);
        }

        .pl-menu button[aria-checked='true'] {
            color: var(--accent-ink);
            font-weight: 600;
        }

        .pl-time {
            flex: none;
            font: 11.5px var(--font-mono);
            font-variant-numeric: tabular-nums;
            color: var(--ink-muted);
        }

        .pl-seek {
            flex: 1;
            min-width: 40px;
            height: 16px;
            margin: 0 4px;
            background: none;
            -webkit-appearance: none;
            appearance: none;
            cursor: pointer;
        }

        .pl-seek::-webkit-slider-runnable-track {
            height: 4px;
            border-radius: 2px;
            /* Fill ends at the thumb's centre, which travels 12px less than the track */
            background:
                linear-gradient(var(--accent), var(--accent)) 0 / calc(6px + (100% - 12px) * var(--f, 0)) 100% no-repeat,
                var(--border);
        }

        .pl-seek::-moz-range-track {
            height: 4px;
            border-radius: 2px;
            background:
                linear-gradient(var(--accent), var(--accent)) 0 / calc(6px + (100% - 12px) * var(--f, 0)) 100% no-repeat,
                var(--border);
        }

        .pl-seek::-webkit-slider-thumb {
            width: 12px;
            height: 12px;
            margin-top: -4px;
            border: 0;
            border-radius: 50%;
            background: var(--accent);
            -webkit-appearance: none;
            appearance: none;
        }

        .pl-seek::-moz-range-thumb {
            width: 12px;
            height: 12px;
            border: 0;
            border-radius: 50%;
            background: var(--accent);
        }

        /* Full screen is always dark, whatever the report theme */
        .player:fullscreen {
            --raised: #181b1e;
            --sunken: #0d0f11;
            --border: #2a2f34;
            --ink: #e6e8ea;
            --ink-muted: #9aa2a9;
            --accent: #2fb3b5;
            --accent-ink: #5cc8ca;
            grid-template-rows: minmax(0, 1fr) auto;
            border: 0;
            border-radius: 0;
        }

        .player:fullscreen video {
            height: 100%;
            max-height: none;
        }

        .files {
            display: grid;
            gap: 6px;
        }

        .file-row {
            display: flex;
            gap: 14px;
            align-items: baseline;
        }

        .file-row a {
            color: var(--accent-ink);
        }

        .file > summary {
            gap: 10px;
            font-size: 12.5px;
        }

        .file > summary .ftype {
            font: 11px var(--font-mono);
            color: var(--ink-muted);
        }

        .kv {
            display: grid;
            grid-template-columns: max-content minmax(0, 1fr);
            gap: 6px 20px;
            margin: 0;
            font-size: 12.5px;
        }

        .kv dt {
            color: var(--ink-muted);
        }

        .kv dd {
            margin: 0;
            font-family: var(--font-mono);
            font-size: 12px;
            overflow-wrap: anywhere;
        }

        .ov-title {
            margin: 0;
            font-size: 18px;
            font-weight: 600;
        }

        .ov-sub {
            margin: 6px 0 0;
            color: var(--ink-muted);
        }

        .slow {
            list-style: none;
            margin: 16px 0 0;
            padding: 0;
            max-width: 720px;
        }

        .slow .item {
            padding: 7px 12px;
            border-radius: var(--radius);
        }

        .slow .suite {
            margin-left: 10px;
            color: var(--ink-muted);
        }

        .empty {
            max-width: 620px;
            padding: 48px 28px;
        }

        .empty h2 {
            margin: 0 0 8px;
            font-size: 18px;
        }

        .empty p {
            max-width: 65ch;
            margin: 0;
            color: var(--ink-muted);
        }

        .empty code {
            color: var(--ink);
        }

        .lightbox {
            width: min(1200px, 94vw);
            max-width: none;
            max-height: 92dvh;
            padding: 0;
            overflow: visible;
            border: 0;
            background: transparent;
            color: #e6e8ea;
        }

        .lightbox::backdrop {
            background: rgb(10 12 14 / 0.9);
        }

        /* Expanded viewer sits directly on the dark backdrop in both themes */
        .is-expanded .car-count,
        .is-expanded .car-file {
            color: rgb(255 255 255 / 0.6);
        }

        .is-expanded .side-btn {
            color: rgb(255 255 255 / 0.7);
        }

        .is-expanded .side-btn:hover {
            color: #fff;
        }

        .is-expanded .ghost {
            color: rgb(255 255 255 / 0.7);
        }

        .is-expanded .ghost:hover {
            background: rgb(255 255 255 / 0.1);
            color: #fff;
        }

        .is-expanded .ghost.goto,
        .is-expanded .ghost.goto:hover {
            color: #5cc8ca;
        }

        .is-expanded .ghost.goto:hover {
            background: rgb(255 255 255 / 0.1);
        }

        .is-expanded .shot-fail {
            color: #f07171;
        }

        .is-expanded .car-thumb img {
            border-color: rgb(255 255 255 / 0.16);
            background: rgb(255 255 255 / 0.06);
        }

        .is-expanded .car-thumb[aria-current] img {
            border-color: #5cc8ca;
            box-shadow: 0 0 0 1px #5cc8ca;
        }

        .is-expanded .car-thumb.is-fail img {
            border-color: #f07171;
            box-shadow: 0 0 0 1px #f07171;
        }

        .is-expanded .car-thumb.is-fail[aria-current] img {
            box-shadow: 0 0 0 2px #f07171;
        }

        .print-only {
            display: none;
        }

        @media (prefers-reduced-motion: no-preference) {
            .detail.enter {
                animation: pr-in 140ms cubic-bezier(0.16, 1, 0.3, 1);
            }

            .dz > summary::before {
                transition: transform 150ms cubic-bezier(0.16, 1, 0.3, 1);
            }

            .bar {
                transition: opacity 120ms;
            }

            .tip {
                transition:
                    opacity 120ms,
                    visibility 120ms;
            }

            ::view-transition-old(root),
            ::view-transition-new(root) {
                animation-duration: 220ms;
                animation-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
            }

            .lightbox[open] {
                animation: pr-pop 180ms cubic-bezier(0.16, 1, 0.3, 1);
            }

            .lightbox[open]::backdrop {
                animation: pr-fade 180ms ease-out;
            }

            .side-btn {
                transition:
                    color 120ms,
                    scale 120ms cubic-bezier(0.16, 1, 0.3, 1);
            }

            .car-thumb,
            .car-thumb img {
                transition:
                    opacity 150ms,
                    border-color 150ms,
                    box-shadow 150ms;
            }

            .car-zoom {
                transition: opacity 200ms;
            }

            .pl-big {
                transition:
                    opacity 150ms,
                    scale 150ms cubic-bezier(0.16, 1, 0.3, 1);
            }

            .step {
                transition:
                    background 600ms ease-out,
                    box-shadow 600ms ease-out;
            }
        }

        @keyframes pr-pop {
            from {
                opacity: 0;
                transform: scale(0.97);
            }
        }

        @keyframes pr-fade {
            from {
                opacity: 0;
            }
        }

        @keyframes pr-in {
            from {
                opacity: 0;
                transform: translateY(4px);
            }

            to {
                opacity: 1;
                transform: none;
            }
        }

        @media screen and (max-width: 767px) {
            .app {
                display: block;
                height: auto;
            }

            .topbar {
                gap: 12px;
                padding: 0 14px;
            }

            .run-meta {
                display: none;
            }

            .verdict {
                grid-template-columns: 1fr;
                gap: 18px;
                padding: 18px 14px;
            }

            .verdict-title {
                font-size: 32px;
            }

            .panes {
                display: block;
            }

            .list-pane {
                border-right: 0;
            }

            .app[data-view='detail'] .list-pane {
                display: none;
            }

            .app[data-view='list'] .detail-pane {
                display: none;
            }

            .back {
                display: inline-flex;
                margin-bottom: 16px;
            }

            .detail {
                padding: 16px 14px 40px;
            }

            .shortcuts {
                display: none;
            }

            .step {
                grid-template-columns: 16px minmax(0, 1fr) auto;
            }

            .step .kw {
                display: none;
            }

            .step-extra {
                grid-column: 2/-1;
            }
        }

        @media print {
            .app {
                display: block;
                height: auto;
            }

            .actions,
            .pulse,
            .panes,
            .tip {
                display: none;
            }

            .topbar,
            .verdict {
                border: 0;
            }

            .print-only {
                display: block;
                padding: 0 20px;
            }

            .pf {
                margin-top: 24px;
                break-inside: avoid-page;
            }

            .pf h3 {
                margin: 0;
                font-size: 14px;
            }

            .pf p {
                margin: 2px 0 10px;
            }

            .msg pre.clamp {
                max-height: none;
                -webkit-mask-image: none;
                mask-image: none;
            }
        }
    </style>
    </#noparse>
</head>
<body>
    <div class="app" id="app" data-view="list">
        <header class="topbar">
            <div class="brand">
                <svg class="brand-mark" viewBox="0 0 256 256" aria-hidden="true" focusable="false">
                    <g class="bm-ink">
                        <circle cx="32" cy="150" r="18"/>
                        <rect x="112" y="132" width="36" height="108" rx="18"/>
                        <rect x="161" y="92" width="36" height="76" rx="18"/>
                        <circle cx="228" cy="150" r="18"/>
                    </g>
                    <path class="bm-accent" d="M63 150 L81 16 L99 150 A18 18 0 0 1 63 150 Z"/>
                </svg>
                <span class="sr-only">PulseReport:</span>
                <h1 class="run-name" id="run-name">${runName}</h1>
            </div>
            <div class="run-meta" id="run-meta"></div>
            <div class="actions">
                <button type="button" class="ghost" id="theme-btn" aria-label="Switch theme"></button>
            </div>
        </header>

        <section class="verdict" id="verdict" aria-label="Run summary"></section>

        <div class="panes">
            <nav class="list-pane" aria-label="Tests">
                <div class="list-controls">
                    <div class="search-wrap">
                        <svg class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                            <circle cx="11" cy="11" r="7"/>
                            <path d="M20 20l-4-4"/>
                        </svg>
                        <input
                            id="search"
                            type="search"
                            aria-label="Search tests"
                            placeholder="Name, tag or error text"
                            autocomplete="off"
                            spellcheck="false">
                    </div>
                    <fieldset class="chips" id="chips">
                        <legend class="sr-only">Filter by status</legend>
                    </fieldset>
                </div>
                <div class="tree" id="tree"></div>
                <p class="shortcuts">
                    <span><kbd>↑</kbd> <kbd>↓</kbd> move</span>
                    <span><kbd>f</kbd> failures only</span>
                    <span><kbd>/</kbd> search</span>
                </p>
            </nav>
            <section class="detail-pane" id="detail" aria-label="Test details">
                <noscript>
                    <div class="empty">
                        <h2>JavaScript is required</h2>
                        <p>This report renders in the browser. Enable JavaScript, or open test-report.json for the raw results.</p>
                    </div>
                </noscript>
            </section>
        </div>
    </div>

    <dialog class="lightbox" id="lightbox" aria-labelledby="lb-cap"></dialog>

    <div class="print-only" id="print-failures"></div>

    <template id="icons">
        <svg data-i="clock" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="9"/>
            <path d="M12 7v5l3 2"/>
        </svg>
        <svg data-i="sun" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="4"/>
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>
        </svg>
        <svg data-i="moon" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M20.37 10.52A8.5 8.5 0 1 1 10.52 3.63a6.6 6.6 0 0 0 9.85 6.89z"/>
        </svg>
        <svg data-i="copy" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="9" y="9" width="11" height="11" rx="2"/>
            <path d="M15 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h3"/>
        </svg>
        <svg data-i="check" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M5 12.5l4.5 4.5L19 7.5"/>
        </svg>
        <svg data-i="monitor" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="3" y="4" width="18" height="12" rx="2"/>
            <path d="M8 20h8M12 16v4"/>
        </svg>
        <svg data-i="phone" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="7" y="2.5" width="10" height="19" rx="2"/>
            <path d="M11 18h2"/>
        </svg>
        <svg data-i="globe" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="9"/>
            <path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>
        </svg>
        <svg data-i="cpu" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="6" y="6" width="12" height="12" rx="2"/>
            <path d="M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4"/>
        </svg>
        <svg data-i="user" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="12" cy="8" r="4"/>
            <path d="M4 21a8 8 0 0 1 16 0"/>
        </svg>
        <svg data-i="server" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="3" y="4" width="18" height="7" rx="2"/>
            <rect x="3" y="13" width="18" height="7" rx="2"/>
            <path d="M7 7.5h.01M7 16.5h.01"/>
        </svg>
        <svg data-i="branch" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="6" cy="5.5" r="2.5"/>
            <circle cx="6" cy="18.5" r="2.5"/>
            <circle cx="18" cy="8" r="2.5"/>
            <path d="M6 8v8M18 10.5c0 4-4 5.5-9.5 6.5"/>
        </svg>
        <svg data-i="layers" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5"/>
        </svg>
        <svg data-i="hash" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M5 9h14M5 15h14M10 4L8 20M16 4l-2 16"/>
        </svg>
        <svg data-i="link" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>
        </svg>
        <svg data-i="flag" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M5 21V4M5 4h12l-2.5 4L17 12H5"/>
        </svg>
        <svg data-i="info" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="9"/>
            <path d="M12 11v5M12 8h.01"/>
        </svg>
        <svg data-i="chev-left" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M15 6l-6 6 6 6"/>
        </svg>
        <svg data-i="chev-right" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M9 6l6 6-6 6"/>
        </svg>
        <svg data-i="close" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M6 6l12 12M18 6L6 18"/>
        </svg>
        <svg data-i="image" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect x="3" y="4" width="18" height="16" rx="2"/>
            <circle cx="9" cy="10" r="1.5"/>
            <path d="M21 16l-5-5-9 9"/>
        </svg>
        <svg data-i="jump" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M19 12H5M11 6l-6 6 6 6"/>
        </svg>
        <svg data-i="play" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path fill="currentColor" d="M8 5.5v13l10.5-6.5z"/>
        </svg>
        <svg data-i="pause" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <rect fill="currentColor" x="6.5" y="5" width="3.5" height="14" rx="1"/>
            <rect fill="currentColor" x="14" y="5" width="3.5" height="14" rx="1"/>
        </svg>
        <svg data-i="expand" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>
        </svg>
        <svg data-i="shrink" class="ic" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5"/>
        </svg>
    </template>

    <script type="application/json" id="pr-data">${runJson?no_esc}</script>
    <#noparse>
    <script>
        (() => {
            'use strict';
            const RUN = JSON.parse(document.getElementById('pr-data').textContent);
            const $ = sel => document.querySelector(sel);
            let gallery = [];
            let inlineViewer = null;
            const stepEls = new WeakMap();
            const wrapIndex = (i, n) => ((i % n) + n) % n;

            const STATUS = {
                PASSED: { key: 'pass', label: 'Passed' },
                FAILED: { key: 'fail', label: 'Failed' },
                SKIPPED: { key: 'skip', label: 'Skipped' }
            };

            const st = s => STATUS[s] || STATUS.SKIPPED;

            // Every piece of run data reaches the DOM as a text node or attribute; nothing is parsed as HTML.
            function h(tag, attrs, ...kids) {
                const el = document.createElement(tag);
                if (attrs) {
                    for (const [k, v] of Object.entries(attrs)) {
                        if (v == null || v === false) continue;
                        if (k === 'class') el.className = v;
                        else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
                        else el.setAttribute(k, v === true ? '' : String(v));
                    }
                }
                for (const kid of kids.flat(Infinity)) {
                    if (kid != null && kid !== false) el.append(kid);
                }
                return el;
            }

            const glyph = s => h('span', { class: `glyph s-${st(s).key}`, 'aria-hidden': 'true' });
            const ICONS = document.getElementById('icons').content;
            const icon = name =>
                (ICONS.querySelector(`[data-i="${name}"]`) || ICONS.querySelector('[data-i="info"]')).cloneNode(true);
            const srStatus = s => h('span', { class: 'sr-only' }, `${st(s).label}: `);

            // ---------- Tooltips (data-tip; first line is the heading) ----------
            const tip = h('div', { class: 'tip', 'aria-hidden': 'true' });
            document.body.append(tip);
            let tipFor = null,
                tipTimer = 0,
                tipHiddenAt = 0;

            function showTip(el) {
                const text = el.dataset.tip;
                if (!text || !el.isConnected) return;
                const [head, ...rest] = text.split('\n');
                tip.replaceChildren(h('strong', null, head));
                if (rest.length) tip.append(h('span', { class: 'tip-sub' }, rest.join('\n')));
                tipFor = el;
                const r = el.getBoundingClientRect();
                const { width, height } = tip.getBoundingClientRect();
                let top = r.bottom + 8;
                if (top + height > innerHeight - 8) top = r.top - height - 8;
                const left = Math.min(Math.max(8, r.left + r.width / 2 - width / 2), innerWidth - width - 8);
                tip.style.transform = `translate(${Math.round(left)}px, ${Math.round(top)}px)`;
                tip.classList.add('on');
            }

            function hideTip() {
                clearTimeout(tipTimer);
                if (tipFor) tipHiddenAt = performance.now();
                tipFor = null;
                tip.classList.remove('on');
            }

            function refreshTip(el) {
                if (tipFor === el || el.matches(':hover')) showTip(el);
            }

            document.addEventListener('pointerover', e => {
                const el = e.target.closest?.('[data-tip]');
                if (el === tipFor) return;
                hideTip();
                if (!el?.dataset.tip) return;
                // Sweeping from one tooltip to the next (e.g. along the timeline) skips the delay.
                tipTimer = setTimeout(() => showTip(el), performance.now() - tipHiddenAt < 300 ? 0 : 600);
            });

            document.addEventListener('pointerdown', hideTip);
            document.addEventListener('focusin', e => {
                const el = e.target.closest?.('[data-tip]');
                if (el?.matches(':focus-visible')) {
                    hideTip();
                    showTip(el);
                }
            });

            document.addEventListener('focusout', hideTip);
            document.addEventListener('scroll', hideTip, { capture: true, passive: true });
            document.addEventListener('keydown', e => {
                if (e.key === 'Escape') hideTip();
            });

            function safeUrl(u) {
                if (!u) return null;
                const s = String(u).trim();
                if (/[\x00-\x1f\x7f]/.test(s)) return null;
                if (/^data:image\/(png|jpe?g|gif|webp|svg\+xml)[;,]/i.test(s)) return s;
                let url;
                try {
                    url = new URL(s, location.href);
                } catch {
                    return null;
                }
                return ['http:', 'https:', 'file:'].includes(url.protocol) ? s : null;
            }

            function fmtDur(ms) {
                if (ms == null) return '';
                if (ms < 1000) return `${Math.round(ms)}ms`;
                if (ms < 60000) return `${(ms / 1000).toFixed(ms < 10000 ? 1 : 0)}s`;
                const total = Math.round(ms / 1000);
                if (total < 3600) return `${Math.floor(total / 60)}m ${String(total % 60).padStart(2, '0')}s`;
                return `${Math.floor(total / 3600)}h ${String(Math.floor(total / 60) % 60).padStart(2, '0')}m`;
            }

            function fmtBytes(n) {
                if (!n) return '';
                const u = ['B', 'KB', 'MB', 'GB'];
                let i = 0;
                while (n >= 1024 && i < u.length - 1) {
                    n /= 1024;
                    i++;
                }
                return `${i && n < 10 ? n.toFixed(1) : Math.round(n)} ${u[i]}`;
            }

            function fmtWhen(iso) {
                const d = new Date(iso);
                return isNaN(d)
                    ? ''
                    : d.toLocaleString(undefined, {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit'
                        });
            }

            const ACRONYMS = { os: 'OS', url: 'URL', id: 'ID', ci: 'CI', api: 'API', jvm: 'JVM', jdk: 'JDK', ip: 'IP' };
            const humanKey = k => {
                const words = String(k)
                    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
                    .split(/[._\s-]+/)
                    .filter(Boolean)
                    .map(w => ACRONYMS[w.toLowerCase()] || w.toLowerCase())
                    .join(' ');
                return words.charAt(0).toUpperCase() + words.slice(1);
            };

            // ---------- Index ----------
            const tests = [];
            for (const suite of RUN.suites || []) {
                for (const tc of suite.testCases || []) {
                    const hay = [
                        tc.name,
                        suite.name,
                        suite.secondaryText,
                        tc.className,
                        tc.methodName,
                        tc.errorMessage,
                        ...(tc.tags || [])
                    ]
                        .filter(Boolean)
                        .join('\n')
                        .toLowerCase();
                    tests.push({ tc, suite, hay });
                }
            }

            const byId = new Map(tests.map(t => [t.tc.id, t]));
            const timeline = [...tests].sort((a, b) => (Date.parse(a.tc.startTime) || 0) - (Date.parse(b.tc.startTime) || 0));
            const MAX_BARS = 400;
            const countOf = s => tests.filter(t => t.tc.status === s).length;
            const totals = {
                all: tests.length,
                FAILED: countOf('FAILED'),
                SKIPPED: countOf('SKIPPED'),
                PASSED: countOf('PASSED')
            };

            const runDuration = RUN.duration || tests.reduce((sum, t) => sum + (t.tc.duration || 0), 0);

            const state = { selected: null, status: 'all', q: '', rawQ: '' };
            const app = $('#app');
            const search = $('#search');
            const itemEls = new Map();
            const barEls = new Map();
            const groups = [];
            const groupOf = new Map();
            const chipEls = [];
            let emptyEl;

            // ---------- Adapter-aware helpers ----------
            const isImage = a => a.type === 'screenshot' || /^image\//i.test(a.mimeType || '');
            const isVideo = a => a.type === 'video' || /^video\//i.test(a.mimeType || '');
            const isHttp = a => a.type === 'http-request' || a.type === 'http-response';

            function allArtifacts(tc) {
                return [
                    ...(tc.artifacts || []),
                    ...[...(tc.backgroundSteps || []), ...(tc.steps || [])].flatMap(s => s.artifacts || [])
                ];
            }

            // Until the model records the adapter, infer it from what the test captured.
            function adapterOf(tc) {
                if (tc.bddType) return 'Cucumber';
                const arts = allArtifacts(tc);
                if (arts.some(isHttp)) return 'REST-assured';
                if (arts.some(a => isImage(a) || isVideo(a)))
                    return /appium/i.test(tc.className || '') || RUN.environment?.['mobile.platform'] ? 'Appium' : 'Selenium';
                return 'TestNG';
            }

            function locationOf(tc, suite) {
                if (tc.bddType) return suite.secondaryText || tc.featureName || suite.name || '';
                if (tc.className) return tc.className.split('.').pop() + (tc.methodName ? `.${tc.methodName}` : '');
                return suite.secondaryText || suite.name || '';
            }

            // Base64 content wins over a path; the generator already inlined readable screenshot files.
            function mediaSrc(a, kind) {
                const c = a.content;
                if (c) {
                    if (c.startsWith('data:')) return c.toLowerCase().startsWith(`data:${kind}/`) ? c : null;
                    const mime = new RegExp(`^${kind}/[\\w.+-]+$`, 'i').test(a.mimeType || '')
                        ? a.mimeType
                        : kind === 'image'
                            ? 'image/png'
                            : 'video/mp4';
                    return `data:${mime};base64,${c.replace(/\s+/g, '')}`;
                }
                return safeUrl(a.path);
            }

            // ---------- Top bar ----------
            const META_ICONS = [
                [/device|mobile|phone/, 'phone'],
                [/browser/, 'globe'],
                [/(^|\.)os(\.|$)|operating|platform/, 'monitor'],
                [/java|jdk|jvm|runtime/, 'cpu'],
                [/user|author|owner/, 'user'],
                [/host|machine|server|node/, 'server'],
                [/branch|commit|git/, 'branch'],
                [/env|stage/, 'layers'],
                [/build|version|ci/, 'hash'],
                [/url|link|endpoint/, 'link'],
                [/finish/, 'flag'],
                [/start|time|date/, 'clock']
            ];
            const metaIcon = k => (META_ICONS.find(([re]) => re.test(String(k).toLowerCase())) || [])[1] || 'info';

            function renderTopbar() {
                const entries = Object.entries(RUN.environment || {}).slice(0, 5);
                const started = fmtWhen(RUN.startTime);
                const finished = fmtWhen(RUN.endTime);
                if (started) entries.push(['Started', started]);
                if (finished) entries.push(['Finished', finished]);
                $('#run-meta').replaceChildren(
                    ...entries.map(([k, v]) =>
                        h(
                            'span',
                            { class: 'meta', 'data-tip': `${humanKey(k)}\n${v}` },
                            icon(metaIcon(k)),
                            h('span', { class: 'sr-only' }, `${humanKey(k)}: `),
                            h('span', { class: 'v' }, v)
                        )
                    )
                );
            }

            // ---------- Verdict + pulse strip ----------
            function renderVerdict() {
                const { all, FAILED: f, SKIPPED: s, PASSED: p } = totals;
                let title,
                    sub,
                    cls = '';
                if (!all) {
                    title = 'No tests recorded';
                    sub = 'The run finished without any test results.';
                } else if (f) {
                    title = `${f} failed`;
                    cls = 'is-fail';
                    sub = `of ${all} ${all === 1 ? 'test' : 'tests'}`;
                } else if (s) {
                    title = 'No failures';
                    sub = `of ${all} tests`;
                } else {
                    title = all === 1 ? '1 test passed' : `All ${all} passed`;
                    sub = `in ${fmtDur(runDuration)}`;
                }

                const parts = [
                    ['PASSED', p],
                    ['SKIPPED', s]
                ].filter(([, n]) => n);
                const left = h(
                    'div',
                    null,
                    h('p', { class: `verdict-title ${cls}` }, title),
                    h('p', { class: 'verdict-sub' }, sub),
                    all && (f || s)
                        ? h(
                                'div',
                                { class: 'breakdown' },
                                parts.map(([k, n]) => h('span', null, glyph(k), h('b', null, String(n)), st(k).label.toLowerCase()))
                            )
                        : null
                );
                $('#verdict').replaceChildren(left, ...(all ? [renderPulse()] : []));
            }

            function renderPulse() {
                const max = timeline.reduce((m, t) => Math.max(m, t.tc.duration || 0), 1);
                const track = h('div', {
                    class: 'pulse-track',
                    role: 'img',
                    'aria-label': `Run timeline: ${totals.all} ${totals.all === 1 ? 'test' : 'tests'} in execution order, bar height shows duration. ${totals.FAILED} failed, ${totals.SKIPPED} skipped.`
                });
                if (timeline.length < 15) track.classList.add('sparse');
                // Large runs share bars: each shows its slowest test and its worst status, and opens that test
                const per = Math.ceil(timeline.length / MAX_BARS);
                if (timeline.length / per > 160) track.style.setProperty('--bar-gap', '1px');
                for (let i = 0; i < timeline.length; i += per) {
                    const chunk = timeline.slice(i, i + per);
                    const worst =
                        chunk.find(t => t.tc.status === 'FAILED') || chunk.find(t => t.tc.status === 'SKIPPED') || chunk[0];
                    const d = chunk.reduce((m, t) => Math.max(m, t.tc.duration || 0), 0);
                    const failed = chunk.filter(t => t.tc.status === 'FAILED').length;
                    const tip =
                        chunk.length === 1
                            ? `${worst.tc.name}\n${st(worst.tc.status).label}, ${fmtDur(d)}`
                            : `Tests ${i + 1}–${i + chunk.length}\n${failed ? `${failed} failed, ` : ''}slowest ${fmtDur(d)}`;
                    const bar = h('div', {
                        class: 'bar',
                        'data-s': st(worst.tc.status).key,
                        'data-id': worst.tc.id,
                        'data-tip': tip
                    });
                    bar.style.height = `${14 + 86 * Math.sqrt(d / max)}%`;
                    for (const t of chunk) barEls.set(t.tc.id, bar);
                    track.append(bar);
                }
                track.addEventListener('click', e => {
                    const bar = e.target.closest('.bar');
                    if (bar) select(bar.dataset.id, { reveal: true });
                });
                const swatch = { PASSED: 'var(--pass)', FAILED: 'var(--fail)', SKIPPED: 'var(--bar-skip)' };
                const legend = h(
                    'div',
                    { class: 'legend', 'aria-hidden': 'true' },
                    ['PASSED', 'FAILED', 'SKIPPED']
                        .filter(k => totals[k])
                        .map(k => {
                            const sw = h('span', { class: 'sw' });
                            sw.style.background = swatch[k];
                            return h('span', null, sw, st(k).label);
                        })
                );
                return h(
                    'div',
                    { class: 'pulse' },
                    track,
                    h(
                        'div',
                        { class: 'pulse-axis' },
                        h('span', { class: 'axis-label' }, 'Run order'),
                        legend,
                        h('span', null, `${fmtDur(runDuration)} total`)
                    )
                );
            }

            // ---------- Test list ----------
            const CHIPS = [
                ['all', 'All'],
                ['FAILED', 'Failed'],
                ['SKIPPED', 'Skipped'],
                ['PASSED', 'Passed']
            ];

            function renderChips() {
                for (const [key, label] of CHIPS) {
                    const n = key === 'all' ? totals.all : totals[key];
                    const chip = h(
                        'button',
                        {
                            type: 'button',
                            class: 'chip',
                            'data-status': key,
                            'aria-pressed': 'false',
                            disabled: key !== 'all' && n === 0
                        },
                        label,
                        h('span', { class: 'n' }, String(n))
                    );
                    chip.addEventListener('click', () => {
                        state.status = key;
                        applyFilter();
                    });
                    chipEls.push(chip);
                }
                $('#chips').append(...chipEls);
            }

            // Rows are built when a suite is first opened, so runs with thousands of tests load quickly
            function renderTree() {
                const nodes = [];
                for (const suite of RUN.suites || []) {
                    const cases = suite.testCases || [];
                    if (!cases.length) continue;
                    const failed = cases.filter(c => c.status === 'FAILED').length;
                    const ul = h('ul', { class: 'items' });
                    const det = h(
                        'details',
                        { class: 'group dz' },
                        h(
                            'summary',
                            { 'data-tip': suite.secondaryText || suite.name || '' },
                            h('span', { class: 'gname' }, suite.name || suite.secondaryText || 'Unnamed suite'),
                            failed
                                ? h('span', { class: 'gbadge' }, `${failed} failed`)
                                : h('span', { class: 'gcount' }, String(cases.length))
                        ),
                        ul
                    );
                    const g = { det, ul, cases, items: [], built: false };
                    det.addEventListener('toggle', () => {
                        if (det.open) buildRows(g);
                    });
                    if (failed) openGroup(g);
                    for (const tc of cases) groupOf.set(tc.id, g);
                    groups.push(g);
                    nodes.push(det);
                }
                emptyEl = h('div', { class: 'tree-empty', hidden: true });
                $('#tree').replaceChildren(...nodes, emptyEl);
            }

            function openGroup(g) {
                buildRows(g);
                g.det.open = true;
            }

            function buildRows(g) {
                if (g.built) return;
                g.built = true;
                for (const tc of g.cases) {
                    const entry = byId.get(tc.id);
                    const btn = h(
                        'button',
                        { type: 'button', class: 'item', 'data-id': tc.id },
                        glyph(tc.status),
                        h('span', { class: 'name' }, srStatus(tc.status), tc.name),
                        h('span', { class: 'dur' }, fmtDur(tc.duration))
                    );
                    btn.addEventListener('click', () => select(tc.id));
                    if (tc.id === state.selected) btn.setAttribute('aria-current', 'true');
                    const li = h('li', { hidden: !matches(entry) }, btn);
                    g.ul.append(li);
                    g.items.push({ li, entry });
                    itemEls.set(tc.id, { li, btn });
                }
            }

            function matches(t) {
                if (state.status !== 'all' && t.tc.status !== state.status) return false;
                return !state.q || t.hay.includes(state.q);
            }

            function applyFilter() {
                const filtering = state.status !== 'all' || state.q;
                let any = 0;
                for (const g of groups) {
                    let hits = 0;
                    for (const tc of g.cases) if (matches(byId.get(tc.id))) hits++;
                    any += hits;
                    g.det.hidden = !hits;
                    if (filtering && hits) openGroup(g);
                    for (const it of g.items) it.li.hidden = !matches(it.entry);
                }
                for (const chip of chipEls) chip.setAttribute('aria-pressed', String(chip.dataset.status === state.status));
                emptyEl.hidden = any > 0;
                if (!any) {
                    const what = state.status === 'all' ? 'tests' : `${st(state.status).label.toLowerCase()} tests`;
                    emptyEl.replaceChildren(
                        h('p', null, state.q ? `No ${what} match “${state.rawQ}”.` : `No ${what} in this run.`),
                        h('button', { type: 'button', class: 'linkbtn', onclick: clearFilters }, 'Clear filters')
                    );
                }
            }

            function clearFilters() {
                state.status = 'all';
                state.q = '';
                state.rawQ = '';
                search.value = '';
                applyFilter();
            }

            let searchTimer = 0;
            search.addEventListener('input', () => {
                state.rawQ = search.value.trim();
                state.q = state.rawQ.toLowerCase();
                clearTimeout(searchTimer);
                // Small runs filter on every keystroke; large ones wait for a pause in typing
                if (tests.length < 2000) applyFilter();
                else searchTimer = setTimeout(applyFilter, 150);
            });

            // ---------- Selection ----------
            function select(id, opts = {}) {
                const entry = byId.get(id);
                if (!entry) return;
                if (state.selected) {
                    itemEls.get(state.selected)?.btn.removeAttribute('aria-current');
                    barEls.get(state.selected)?.removeAttribute('aria-current');
                }
                state.selected = id;
                if (opts.reveal) {
                    const g = groupOf.get(id);
                    if (g) openGroup(g);
                }
                const it = itemEls.get(id);
                if (it) {
                    it.btn.setAttribute('aria-current', 'true');
                    if (opts.reveal) {
                        if (it.li.hidden) clearFilters();
                        it.btn.scrollIntoView({ block: 'nearest' });
                    }
                }
                barEls.get(id)?.setAttribute('aria-current', 'true');
                renderDetail(entry);
                if (!opts.initial) {
                    setHash();
                    app.dataset.view = 'detail';
                    if (matchMedia('(max-width: 767px)').matches) $('#detail').scrollIntoView();
                }
            }

            // #t=<test id>[&s=<screenshot number>]
            function setHash(shot) {
                if (!state.selected) return;
                try {
                    history.replaceState(null, '', `#t=${encodeURIComponent(state.selected)}${shot ? `&s=${shot}` : ''}`);
                } catch {
                    /* file:// in some browsers */
                }
            }

            function idFromHash() {
                const m = location.hash.match(/^#t=([^&]+)/);
                if (!m) return null;
                try {
                    return decodeURIComponent(m[1]);
                } catch {
                    return null;
                }
            }

            const shotFromHash = () => Number(location.hash.match(/&s=(\d+)$/)?.[1]) || 0;

            function openShotFromHash(shot) {
                if (shot && gallery[shot - 1]) showLightbox(gallery, shot - 1);
            }

            // ---------- Detail ----------
            const section = (title, ...kids) => h('section', { class: 'section' }, h('h3', null, title), ...kids);

            function renderDetail({ tc, suite }) {
                const d = h('div', { class: 'detail enter' });
                d.append(
                    h(
                        'button',
                        {
                            type: 'button',
                            class: 'btn back',
                            onclick: () => {
                                app.dataset.view = 'list';
                                itemEls.get(tc.id)?.btn.focus();
                            }
                        },
                        '← All tests'
                    )
                );
                d.append(renderHead(tc, suite));

                const failure = renderFailure(tc);
                if (failure) d.append(section(tc.status === 'SKIPPED' ? 'Skip reason' : 'Failure', failure));
                gallery = galleryOf(tc);
                const steps = renderSteps(tc);
                if (steps)
                    d.append(
                        section('Steps', tc.featureDescription ? h('p', { class: 'desc' }, tc.featureDescription) : null, steps)
                    );
                const arts = tc.artifacts || [];
                const exchanges = pairExchanges(arts);
                if (exchanges.length)
                    d.append(
                        section(
                            exchanges.length > 1 ? 'HTTP exchanges' : 'HTTP exchange',
                            exchanges.length > 1 ? h('div', { class: 'xlist' }, exchanges.map(exchangeDetails)) : exchangeEl(exchanges[0])
                        )
                    );
                inlineViewer = gallery.length ? createViewer(gallery, { start: startShot(tc, gallery) }) : null;
                const shots = inlineViewer ? inlineViewer.root : renderShots(arts);
                if (shots) d.append(section('Screenshots', shots));
                const videos = renderVideos(arts);
                if (videos) d.append(section('Recordings', videos));
                const files = renderFiles(arts);
                if (files) d.append(section('Attachments', files));
                d.append(section('Details', renderKv(tc, suite)));

                const pane = $('#detail');
                pane.replaceChildren(d);
                pane.scrollTop = 0;
            }

            function renderHead(tc, suite) {
                const s = st(tc.status);
                const tags = tc.tags || [];
                return h(
                    'header',
                    { class: 'd-head' },
                    h('h2', null, h('span', { class: 'sr-only' }, `${s.label}: `), tc.name),
                    h(
                        'div',
                        { class: 'd-meta' },
                        h('span', { class: 'pill' }, adapterOf(tc)),
                        h('span', { class: 'mono' }, locationOf(tc, suite)),
                        h('span', { class: 'mono' }, fmtDur(tc.duration)),
                        tc.retryCount ? h('span', null, `Retried ${tc.retryCount}×`) : null
                    ),
                    tags.length
                        ? h(
                                'div',
                                { class: 'tags' },
                                tags.map(t => h('span', { class: 'tag' }, t))
                            )
                        : null
                );
            }

            // ---------- Failure + stack trace ----------
            const FRAMEWORK =
                /^(?:java\.base\/|java\.|javax\.|jdk\.|sun\.|com\.sun\.|org\.testng\.|org\.junit\.|junit\.|org\.opentest4j\.|io\.cucumber\.|org\.codehaus\.groovy\.|groovy\.|io\.restassured\.|org\.apache\.|org\.openqa\.selenium\.support\.ui\.|org\.openqa\.selenium\.remote\.|io\.appium\.java_client\.|org\.gradle\.|org\.springframework\.|io\.github\.pulsereport\.(?:adapters|core|outputs)\.)/;
            const isFramework = f => FRAMEWORK.test(f) || /^\.\.\. \d+ more$/.test(f);

            function parseTrace(raw) {
                const head = [],
                    frames = [];
                for (const line of raw.split('\n')) {
                    const m = line.match(/^\s*at\s+(.+)$/);
                    if (m) frames.push(m[1].trim());
                    else if (!frames.length) head.push(line);
                    else if (line.trim()) frames.push(line.trim());
                }
                return { head, frames };
            }

            function exceptionName(line) {
                const m = (line || '').match(/^([\w$.]+(?:Exception|Error|Throwable))\b/);
                return m ? m[1].split('.').pop() : '';
            }

            function renderFailure(tc, { open = false } = {}) {
                if (tc.status === 'PASSED' || (!tc.errorMessage && !tc.stackTrace)) return null;
                const { head, frames } = parseTrace(tc.stackTrace || '');
                const message = (tc.errorMessage || head.join('\n')).trim();
                const exc = exceptionName(head[0]);
                const pre = h('pre', null, message);
                const box = h('div', { class: 'msg' }, exc ? h('div', { class: 'exc' }, exc) : null, pre);
                if (!open && (message.split('\n').length > 4 || message.length > 420)) {
                    pre.classList.add('clamp');
                    const more = h('button', { type: 'button', class: 'linkbtn' }, 'Show full message');
                    more.addEventListener('click', () => {
                        more.textContent = pre.classList.toggle('clamp') ? 'Show full message' : 'Show less';
                    });
                    box.append(more);
                }
                const panel = h('div', { class: `panel failure${tc.status === 'SKIPPED' ? ' is-skip' : ''}` }, box);
                if (frames.length) panel.append(renderTrace(tc.stackTrace, frames, open));
                return panel;
            }

            function renderTrace(raw, frames, open) {
                const copy = h(
                    'button',
                    { type: 'button', class: 'ghost', 'data-tip': 'Copy stack trace', 'aria-label': 'Copy stack trace' },
                    icon('copy')
                );
                const setCopy = (name, label, ok) => {
                    copy.replaceChildren(icon(name));
                    copy.dataset.tip = label;
                    copy.setAttribute('aria-label', label);
                    copy.classList.toggle('ok', ok);
                    refreshTip(copy);
                };
                copy.addEventListener('click', async () => {
                    try {
                        await navigator.clipboard.writeText(raw);
                        setCopy('check', 'Copied', true);
                    } catch {
                        setCopy('copy', 'Copy is not allowed on this page', false);
                    }
                    setTimeout(() => setCopy('copy', 'Copy stack trace', false), 1600);
                });
                const wrap = h(
                    'div',
                    { class: 'trace' },
                    h('div', { class: 'trace-head' }, h('span', null, `Stack trace, ${frames.length} frames`), copy)
                );
                const frameEl = (f, fw) =>
                    h(
                        'div',
                        { class: `frame${fw ? ' fw' : ''}${/^Caused by:/.test(f) ? ' cause' : ''}` },
                        /^(Caused by:|\.\.\.)/.test(f) ? f : `at ${f}`
                    );
                let buf = [];
                const flush = () => {
                    if (buf.length === 1) wrap.append(frameEl(buf[0], true));
                    else if (buf.length)
                        wrap.append(
                            h(
                                'details',
                                { class: 'dz frames-fw', open },
                                h('summary', null, `${buf.length} framework frames`),
                                buf.map(f => frameEl(f, true))
                            )
                        );
                    buf = [];
                };
                for (const f of frames) {
                    if (isFramework(f)) buf.push(f);
                    else {
                        flush();
                        wrap.append(frameEl(f, false));
                    }
                }
                flush();
                return wrap;
            }

            // ---------- Steps ----------
            function renderSteps(tc) {
                const bg = tc.backgroundSteps || [],
                    steps = tc.steps || [];
                if (!bg.length && !steps.length) return null;
                const ctx = { testError: tc.errorMessage };
                const panel = h('div', { class: 'panel' });
                if (bg.length) {
                    const bgOk = bg.every(s => s.status === 'PASSED');
                    const label = `Background, ${bg.length} ${bg.length === 1 ? 'step' : 'steps'}${bgOk ? ' passed' : ''}`;
                    panel.append(
                        h(
                            'details',
                            { class: 'dz bg', open: !bgOk },
                            h('summary', null, label),
                            h(
                                'ol',
                                { class: 'steps' },
                                bg.map(s => stepEl(s, ctx))
                            )
                        )
                    );
                }
                if (steps.length)
                    panel.append(
                        h(
                            'ol',
                            { class: 'steps' },
                            steps.map(s => stepEl(s, ctx))
                        )
                    );
                return panel;
            }

            function stepText(text) {
                return text
                    .split(/("[^"]*")/)
                    .filter(Boolean)
                    .map(part =>
                        part.length > 1 && part.startsWith('"') && part.endsWith('"') ? h('span', { class: 'arg' }, part) : part
                    );
            }

            function stepEl(s, ctx) {
                const failed = s.status === 'FAILED';
                const li = h(
                    'li',
                    { class: `step${failed ? ' is-fail' : ''}` },
                    glyph(s.status),
                    h('span', { class: 'kw' }, s.keyword || ''),
                    h(
                        'span',
                        { class: 'stext' },
                        srStatus(s.status),
                        stepText(s.name || ''),
                        s.description ? h('span', { class: 'step-desc' }, s.description) : null
                    ),
                    h('span', { class: 'step-end' }, stepShotBtn(s.artifacts || []), h('span', { class: 'dur' }, s.duration ? fmtDur(s.duration) : ''))
                );
                const arts = s.artifacts || [];
                const asserts = s.assertions || [];
                const extra = [];
                const errShown = s.errorMessage === ctx.testError || asserts.some(a => a.errorMessage === s.errorMessage);
                if (s.errorMessage && !errShown) extra.push(h('pre', { class: 'step-err' }, s.errorMessage));
                if (s.dataTable?.length) extra.push(dataTable(s.dataTable));
                if (s.docString) extra.push(h('pre', { class: 'body' }, s.docString));
                extra.push(assertionList(asserts));
                for (const pair of pairExchanges(arts)) extra.push(exchangeDetails(pair));
                extra.push(stepShots(arts), renderVideos(arts), renderFiles(arts));
                const present = extra.filter(Boolean);
                if (present.length) li.append(h('div', { class: 'step-extra' }, present));
                stepEls.set(s, li);
                return li;
            }

            function assertionList(list) {
                if (!list.length) return null;
                const failed = list.filter(a => a.status === 'FAILED').length;
                const noun = list.length === 1 ? 'assertion' : 'assertions';
                const label = failed
                    ? `${list.length} ${noun}, ${failed} failed`
                    : `${list.length} ${noun} passed`;
                const row = a =>
                    h(
                        'tr',
                        { class: a.status === 'FAILED' ? 'is-fail' : null },
                        h('td', { class: 'a-text' }, srStatus(a.status), a.message || a.method),
                        h('td', { class: 'a-exp', 'data-label': 'expected' }, a.expected),
                        h('td', { class: 'a-act', 'data-label': 'actual' }, a.actual),
                        h('td', { class: 'a-fn', 'data-tip': [a.method, a.framework].filter(Boolean).join('\n') }, a.method)
                    );
                return h(
                    'details',
                    { class: 'dz awrap', open: failed > 0 },
                    h('summary', null, label),
                    h(
                        'table',
                        { class: 'dt at' },
                        h('colgroup', null, ['c-msg', 'c-exp', 'c-act', 'c-fn'].map(c => h('col', { class: c }))),
                        h(
                            'thead',
                            null,
                            h(
                                'tr',
                                null,
                                ['Assertion', 'Expected', 'Actual'].map(t => h('th', { scope: 'col' }, t)),
                                h('th', { scope: 'col' }, h('span', { class: 'sr-only' }, 'Method'))
                            )
                        ),
                        h('tbody', null, list.map(row))
                    )
                );
            }

            function goToStep(step) {
                const li = stepEls.get(step);
                if (!li?.isConnected) return;
                const bg = li.closest('details');
                if (bg) bg.open = true;
                li.tabIndex = -1;
                li.scrollIntoView({ block: 'center' });
                li.focus({ preventScroll: true });
                li.classList.add('flash');
                setTimeout(() => li.classList.remove('flash'), 2000);
            }

            function dataTable(rows) {
                const [head, ...body] = rows;
                const numbered = body.length > 1;
                return h(
                    'div',
                    { class: 'dt-scroll' },
                    h(
                        'table',
                        { class: 'dt' },
                        h(
                            'thead',
                            null,
                            h(
                                'tr',
                                null,
                                numbered ? h('th', { scope: 'col', class: 'dt-n' }, '#') : null,
                                head.map(c => h('th', { scope: 'col' }, c))
                            )
                        ),
                        h(
                            'tbody',
                            null,
                            body.map((r, i) =>
                                h(
                                    'tr',
                                    null,
                                    numbered ? h('td', { class: 'dt-n' }, String(i + 1)) : null,
                                    r.map(c => h('td', null, c))
                                )
                            )
                        )
                    )
                );
            }

            // ---------- HTTP ----------
            const REASONS = {
                200: 'OK',
                201: 'Created',
                202: 'Accepted',
                204: 'No Content',
                301: 'Moved Permanently',
                302: 'Found',
                304: 'Not Modified',
                400: 'Bad Request',
                401: 'Unauthorized',
                403: 'Forbidden',
                404: 'Not Found',
                409: 'Conflict',
                422: 'Unprocessable Content',
                429: 'Too Many Requests',
                500: 'Internal Server Error',
                502: 'Bad Gateway',
                503: 'Service Unavailable',
                504: 'Gateway Timeout'
            };

            // Format written by RestAssuredAdapter: "<line>\n\nHeaders:\n...\n\nBody:\n...", optionally followed by a truncation note.
            function parseHttp(content) {
                const out = { line: '', headers: [], body: '', note: '' };
                let text = content || '';
                const cut = text.match(/\n*\[(Content truncated[^\]]*)\]\s*$/);
                if (cut) {
                    out.note = cut[1];
                    text = text.slice(0, cut.index);
                }
                const hm = /\n\nHeaders:\n/.exec(text);
                const bm = /\n\nBody:?(?:\n|$)/.exec(text);
                const end = bm ? bm.index : text.length;
                out.line = text.slice(0, hm ? hm.index : end).trim();
                if (hm && hm.index < end) {
                    out.headers = text
                        .slice(hm.index + hm[0].length, end)
                        .split('\n')
                        .filter(Boolean)
                        .map(l => {
                            const i = l.indexOf(':');
                            return i > 0 ? [l.slice(0, i), l.slice(i + 1).trim()] : [l, ''];
                        });
                }
                if (bm) out.body = text.slice(bm.index + bm[0].length).trim();
                return out;
            }

            function pairExchanges(arts) {
                const out = [];
                let cur = null;
                for (const a of arts) {
                    if (a.type === 'http-request') {
                        cur = { req: parseHttp(a.content || ''), res: null };
                        out.push(cur);
                    } else if (a.type === 'http-response') {
                        if (cur && !cur.res) cur.res = parseHttp(a.content || '');
                        else out.push({ req: null, res: parseHttp(a.content || '') });
                    }
                }
                return out;
            }

            const statusCode = res => +((res.line.match(/\b\d{3}\b/) || [])[0] || 0);
            const statusClass = code => (code >= 400 ? 's-fail' : code >= 200 && code < 300 ? 's-pass' : '');

            function exchangeTitle({ req, res }) {
                let title = 'HTTP';
                if (req) {
                    const [method, url] = req.line.split(/\s+/);
                    let path = url || '';
                    try {
                        path = new URL(url).pathname;
                    } catch {
                        /* keep raw */
                    }
                    title = `${method} ${path}`;
                }
                if (res) {
                    const code = statusCode(res);
                    return [`${title} → `, h('span', { class: `xcode ${statusClass(code)}` }, code ? String(code) : res.line)];
                }
                return req ? [`${title} → `, h('span', { class: 'xcode s-fail' }, 'no response')] : title;
            }

            function prettyBody(body) {
                try {
                    return JSON.stringify(JSON.parse(body), null, 2);
                } catch {
                    return body;
                }
            }

            function httpSide(label, p, isResponse) {
                if (!p)
                    return h(
                        'div',
                        { class: 'xside' },
                        h('div', { class: 'xlabel' }, label),
                        h('p', { class: 'xnone' }, isResponse ? 'No response received.' : 'Request was not captured.')
                    );
                let line;
                if (isResponse) {
                    const code = statusCode(p);
                    line = h('div', { class: `xline xstatus ${statusClass(code)}` }, code ? `${code} ${REASONS[code] || ''}`.trim() : p.line);
                } else {
                    line = h('div', { class: 'xline' }, p.line);
                }
                return h(
                    'div',
                    { class: 'xside' },
                    h('div', { class: 'xlabel' }, label),
                    line,
                    p.headers.length
                        ? h(
                                'details',
                                { class: 'dz hdrs' },
                                h('summary', null, `Headers (${p.headers.length})`),
                                h(
                                    'dl',
                                    null,
                                    p.headers.flatMap(([k, v]) => [h('dt', null, k), h('dd', null, v)])
                                )
                            )
                        : null,
                    p.body ? h('pre', { class: 'body' }, prettyBody(p.body)) : null,
                    p.note ? h('p', { class: 'xnote' }, `${p.note}. Raise reporter.maxArtifactContentSize to capture more.`) : null
                );
            }

            const exchangeEl = pair =>
                h('div', { class: 'xchg' }, httpSide('Request', pair.req, false), httpSide('Response', pair.res, true));

            const exchangeDetails = pair =>
                h('details', { class: 'dz xwrap' }, h('summary', null, exchangeTitle(pair)), exchangeEl(pair));

            // ---------- Artifacts ----------
            function renderShots(arts) {
                const shots = arts.filter(isImage);
                if (!shots.length) return null;
                return h(
                    'div',
                    { class: 'shots' },
                    shots.map(a => {
                        const src = mediaSrc(a, 'image');
                        const name = a.name || 'screenshot';
                        if (!src)
                            return h(
                                'div',
                                null,
                                h('div', { class: 'shot-missing' }, 'Image not embedded'),
                                h('span', { class: 'cap', 'data-tip': a.path || '' }, name)
                            );
                        const btn = h(
                            'button',
                            { type: 'button', class: 'shot', 'aria-label': `Open screenshot ${name}` },
                            h('img', { src, alt: name, loading: 'lazy' }),
                            h('span', { class: 'cap' }, name)
                        );
                        btn.addEventListener('click', () => {
                            const i = gallery.findIndex(g => g.a === a);
                            if (i >= 0) showLightbox(gallery, i);
                            else showLightbox([{ src, name }], 0);
                        });
                        return btn;
                    })
                );
            }

            // Embedded screenshots open from the step's icon; only non-embedded ones are listed under the step
            function stepShots(arts) {
                const missing = arts.filter(a => isImage(a) && !mediaSrc(a, 'image'));
                const shots = renderShots(missing);
                if (!shots) return null;
                const n = missing.length;
                return h('details', { class: 'dz step-shots' }, h('summary', null, n === 1 ? 'Screenshot' : `Screenshots (${n})`), shots);
            }

            function stepShotBtn(arts) {
                const shots = arts.filter(a => isImage(a) && mediaSrc(a, 'image'));
                if (!shots.length) return null;
                const n = shots.length;
                const label = n === 1 ? 'Open screenshot' : `Open screenshots (${n})`;
                return h(
                    'button',
                    {
                        type: 'button',
                        class: 'ghost step-shot',
                        'aria-label': label,
                        'data-tip': label,
                        onclick: () => {
                            const i = gallery.findIndex(g => g.a === shots[0]);
                            if (i >= 0) showLightbox(gallery, i);
                            else showLightbox(shots.map(a => ({ src: mediaSrc(a, 'image'), name: a.name || 'screenshot' })), 0);
                        }
                    },
                    icon('image'),
                    n > 1 ? String(n) : null
                );
            }

            // ---------- Screenshot gallery: every screenshot of the open test, in capture order ----------
            function galleryOf(tc) {
                const items = [];
                const add = (arts, step) => {
                    for (const a of (arts || []).filter(isImage)) {
                        const src = mediaSrc(a, 'image');
                        if (!src) continue;
                        const name = a.name || 'screenshot';
                        items.push({
                            a,
                            src,
                            name,
                            step: step?.name || null,
                            stepRef: step,
                            failed: step ? step.status === 'FAILED' : tc.status === 'FAILED' && /fail/i.test(name),
                            order: items.length,
                            ts: Date.parse(a.timestamp || '')
                        });
                    }
                };
                for (const s of [...(tc.backgroundSteps || []), ...(tc.steps || [])]) add(s.artifacts, s);
                add(tc.artifacts, null);
                if (items.every(it => Number.isFinite(it.ts))) items.sort((x, y) => x.ts - y.ts || x.order - y.order);
                return items;
            }

            // Failed tests open on the failure evidence rather than the first screenshot
            function startShot(tc, items) {
                if (tc.status !== 'FAILED') return 0;
                const i = items.findIndex(it => it.failed);
                return i >= 0 ? i : items.length - 1;
            }

            // Touch/pen swipe; mouse users have the arrows
            function onSwipe(el, go, enabled = () => true) {
                let x0 = null;
                el.addEventListener('pointerdown', e => {
                    x0 = e.pointerType !== 'mouse' && enabled() ? e.clientX : null;
                });
                el.addEventListener('pointerup', e => {
                    if (x0 == null) return;
                    const dx = e.clientX - x0;
                    x0 = null;
                    if (Math.abs(dx) > 40) go(dx < 0 ? 1 : -1);
                });
                el.addEventListener('pointercancel', () => (x0 = null));
            }

            const navKey = (key, idx, n) => ({ ArrowLeft: idx - 1, ArrowRight: idx + 1, Home: 0, End: n - 1 })[key];
            const reduceMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
            const EASE = 'cubic-bezier(0.16, 1, 0.3, 1)';
            const MAX_ZOOM = 8;

            // One viewer: rendered inline under Screenshots and expanded (same component) in the dialog
            function createViewer(items, { start = 0, expanded = false } = {}) {
                let idx = -1;
                const many = items.length > 1;
                const img = h('img', { alt: '', draggable: 'false' });
                const view = expanded
                    ? h('div', { class: 'car-view' }, img)
                    : h('button', { type: 'button', class: 'car-view', onclick: () => showLightbox(items, idx) }, img);
                // Mouse presses keep focus on the viewer itself, so no focus ring appears on the button
                const keepFocus = e => {
                    e.preventDefault();
                    root.focus({ preventScroll: true });
                };
                const sideBtn = (cls, label, ic, onclick) =>
                    h('button', { type: 'button', class: `side-btn ${cls}`, 'aria-label': label, onmousedown: keepFocus, onclick }, icon(ic));
                const prev = many ? sideBtn('prev', 'Previous screenshot', 'chev-left', () => show(idx - 1)) : null;
                const next = many ? sideBtn('next', 'Next screenshot', 'chev-right', () => show(idx + 1)) : null;
                const close = expanded ? sideBtn('close', 'Close', 'close', closeLightbox) : null;
                const zoomBadge = expanded ? h('span', { class: 'car-zoom', 'aria-hidden': 'true' }) : null;
                const frame = h('div', { class: 'car-frame' }, view, zoomBadge);
                const stage = h('div', { class: 'car-stage' }, prev, frame, next);

                const count = h('span', { class: 'car-count' });
                const fail = h('span', { class: 'shot-fail', hidden: true }, 'Failure');
                const cap = h('span', { class: 'car-cap', id: expanded ? 'lb-cap' : null });
                const live = h('span', { class: 'car-live', 'aria-live': 'polite' }, count, fail, cap);
                const file = h('span', { class: 'car-file' });
                const goStep = h(
                    'button',
                    {
                        type: 'button',
                        class: 'ghost goto',
                        'aria-label': 'Go to step',
                        'data-tip': 'Go to step',
                        onclick: () => {
                            const step = items[idx].stepRef;
                            if (expanded) closeLightbox();
                            goToStep(step);
                        }
                    },
                    icon('jump')
                );

                const thumbs = items.map((it, i) =>
                    h(
                        'button',
                        {
                            type: 'button',
                            class: `car-thumb${it.failed ? ' is-fail' : ''}`,
                            tabindex: '-1',
                            'aria-label': `Screenshot ${i + 1}: ${it.failed ? 'failure, ' : ''}${it.step || it.name}`,
                            onmousedown: keepFocus,
                            onclick: () => show(i)
                        },
                        h('img', { src: it.src, alt: '', loading: 'lazy', draggable: 'false' }),
                        h('span', { class: 'car-num', 'aria-hidden': 'true' }, String(i + 1))
                    )
                );
                // One tab stop for the whole strip; arrow keys move within it
                const strip = many ? h('div', { class: 'car-thumbs', role: 'group', 'aria-label': 'Choose screenshot' }, thumbs) : null;

                const root = h(
                    'div',
                    {
                        class: `carousel${expanded ? ' is-expanded' : ''}`,
                        role: 'group',
                        'aria-roledescription': 'carousel',
                        'aria-label': 'Screenshots',
                        tabindex: '-1'
                    },
                    // Caption row on top (with close when expanded), aligned to the image and arrow columns
                    h('div', { class: 'car-head' }, h('div', { class: 'car-meta' }, live, goStep, file, close)),
                    stage,
                    strip
                );

                // ---- Zoom (expanded only): wheel / trackpad pinch at the cursor, drag to pan, double-click to toggle ----
                let scale = 1,
                    tx = 0,
                    ty = 0,
                    badgeTimer = 0;

                function paint(badge = true) {
                    img.style.transform = scale > 1 ? `translate(${tx}px, ${ty}px) scale(${scale})` : '';
                    frame.classList.toggle('is-zoomed', scale > 1);
                    if (badge && zoomBadge) {
                        zoomBadge.textContent = `${Math.round(scale * 100)}%`;
                        zoomBadge.classList.add('show');
                        clearTimeout(badgeTimer);
                        badgeTimer = setTimeout(() => zoomBadge.classList.remove('show'), 900);
                    }
                }

                function clampPan() {
                    const w = img.offsetWidth,
                        ht = img.offsetHeight;
                    tx = Math.min(0, Math.max(w - w * scale, tx));
                    ty = Math.min(0, Math.max(ht - ht * scale, ty));
                }

                function zoomAt(cx, cy, wanted, smooth = false) {
                    const ns = Math.min(MAX_ZOOM, Math.max(1, wanted));
                    if (ns === scale) return;
                    const r = img.getBoundingClientRect();
                    const lx = (cx - r.left) / scale,
                        ly = (cy - r.top) / scale;
                    const ox = r.left - tx,
                        oy = r.top - ty;
                    scale = ns;
                    tx = cx - ox - lx * scale;
                    ty = cy - oy - ly * scale;
                    clampPan();
                    if (smooth && !reduceMotion()) {
                        img.style.transition = `transform 200ms ${EASE}`;
                        setTimeout(() => (img.style.transition = ''), 220);
                    }
                    paint();
                }

                function resetZoom() {
                    scale = 1;
                    tx = ty = 0;
                    paint(false);
                }

                if (expanded) {
                    view.addEventListener(
                        'wheel',
                        e => {
                            e.preventDefault();
                            if (!e.ctrlKey && scale > 1 && Math.abs(e.deltaX) > Math.abs(e.deltaY)) {
                                tx -= e.deltaX;
                                clampPan();
                                paint(false);
                                return;
                            }
                            const delta = e.deltaMode === 1 ? e.deltaY * 16 : e.deltaY;
                            // Trackpad pinch arrives as ctrl+wheel with small deltas
                            zoomAt(e.clientX, e.clientY, scale * Math.exp(-delta * (e.ctrlKey ? 0.01 : 0.002)));
                        },
                        { passive: false }
                    );
                    let drag = null;
                    view.addEventListener('pointerdown', e => {
                        if (scale === 1 || e.button !== 0) return;
                        drag = { x: e.clientX, y: e.clientY, tx, ty };
                        view.setPointerCapture(e.pointerId);
                        frame.classList.add('is-panning');
                    });
                    view.addEventListener('pointermove', e => {
                        if (!drag) return;
                        tx = drag.tx + e.clientX - drag.x;
                        ty = drag.ty + e.clientY - drag.y;
                        clampPan();
                        paint(false);
                    });
                    const endDrag = () => {
                        drag = null;
                        frame.classList.remove('is-panning');
                    };
                    view.addEventListener('pointerup', endDrag);
                    view.addEventListener('pointercancel', endDrag);
                    view.addEventListener('dblclick', e => {
                        if (scale > 1) zoomAt(e.clientX, e.clientY, 1, true);
                        else zoomAt(e.clientX, e.clientY, 2.5, true);
                    });
                }

                // Outgoing screenshot slides out while the new one slides in from the other side
                let ghost = null,
                    slideToken = 0;

                function stopSlide() {
                    slideToken++;
                    ghost?.remove();
                    ghost = null;
                    view.getAnimations().forEach(a => a.cancel());
                    view.style.transform = '';
                }

                function slide(dir) {
                    const token = slideToken;
                    const g = view.cloneNode(true);
                    g.className = 'car-ghost car-view';
                    g.setAttribute('aria-hidden', 'true');
                    g.inert = true;
                    frame.append(g);
                    ghost = g;
                    // 1px overlap so anti-aliased edges never leave a hairline at the seam
                    const from = `translateX(calc(${dir * 100}% - ${dir}px))`;
                    view.style.transform = from;
                    const run = () => {
                        if (token !== slideToken) return;
                        view.style.transform = '';
                        const timing = { duration: 320, easing: EASE };
                        view.animate([{ transform: from }, { transform: 'none' }], timing);
                        const out = g.animate([{ transform: 'none' }, { transform: `translateX(calc(${-dir * 100}% + ${dir}px))` }], timing);
                        out.finished.then(
                            () => g.remove(),
                            () => g.remove()
                        );
                    };
                    return { run };
                }

                // Size the image column to the rendered screenshot so header, arrows and thumbnails hug it
                function fitWidth() {
                    if (!img.naturalWidth || !root.isConnected) return;
                    const avail = root.clientWidth - 2 * 46;
                    const maxH = parseFloat(getComputedStyle(img).maxHeight) || img.naturalHeight;
                    const scale = Math.min(1, maxH / img.naturalHeight, avail / img.naturalWidth);
                    root.style.setProperty('--shot-w', `${Math.floor(img.naturalWidth * scale)}px`);
                }

                function show(i, { animate = true } = {}) {
                    const dir = idx < 0 ? 0 : Math.sign(i - idx);
                    const thumbFocused = thumbs.includes(document.activeElement);
                    stopSlide();
                    const sliding = animate && dir && !reduceMotion() ? slide(dir) : null;
                    idx = wrapIndex(i, items.length);
                    const it = items[idx];
                    resetZoom();
                    img.src = it.src;
                    img.decode().then(fitWidth, () => {});
                    // Wait for the new image to decode so it never slides in blank
                    if (sliding) img.decode().then(sliding.run, sliding.run);
                    img.alt = it.step || it.name;
                    if (!expanded) view.setAttribute('aria-label', `Open screenshot ${idx + 1} of ${items.length} full size`);
                    count.textContent = many ? `${idx + 1} / ${items.length}` : '';
                    fail.hidden = !it.failed;
                    cap.textContent = it.step || 'Test';
                    file.textContent = it.name;
                    goStep.hidden = !it.stepRef;
                    thumbs.forEach((t, j) => {
                        t.tabIndex = j === idx ? 0 : -1;
                        if (j === idx) t.setAttribute('aria-current', 'true');
                        else t.removeAttribute('aria-current');
                    });
                    if (thumbFocused) thumbs[idx].focus({ preventScroll: true });
                    if (strip)
                        requestAnimationFrame(() => {
                            const t = thumbs[idx];
                            strip.scrollTo({
                                left: t.offsetLeft - (strip.clientWidth - t.clientWidth) / 2,
                                behavior: animate && !reduceMotion() ? 'smooth' : 'auto'
                            });
                        });
                    if (expanded && items === gallery) setHash(idx + 1);
                }

                function handleKey(e) {
                    if (expanded && ['+', '=', '-', '0'].includes(e.key)) {
                        e.preventDefault();
                        const r = view.getBoundingClientRect();
                        const factor = e.key === '-' ? 1 / 1.5 : e.key === '0' ? 0 : 1.5;
                        zoomAt(r.left + r.width / 2, r.top + r.height / 2, factor ? scale * factor : 1, true);
                        return;
                    }
                    const target = navKey(e.key, idx, items.length);
                    if (!many || target === undefined) return;
                    e.preventDefault();
                    show(target);
                }

                root.addEventListener('keydown', handleKey);
                new ResizeObserver(fitWidth).observe(root);
                if (many) onSwipe(view, dir => show(idx + dir), () => scale === 1);
                show(start, { animate: false });
                return {
                    root,
                    items,
                    show,
                    handleKey,
                    get index() {
                        return idx;
                    }
                };
            }

            const fmtClock = sec => {
                const s = Math.max(0, Math.floor(sec || 0));
                return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
            };
            // Listed top to bottom, so the menu reads 0.5× up to 2× from the button
            const RATES = [2, 1.5, 1, 0.5];

            function createPlayer(src, name) {
                const video = h('video', { src, preload: 'metadata', playsinline: true });
                const screen = h('div', { class: 'pl-screen' }, video, h('span', { class: 'pl-big', 'aria-hidden': 'true' }, icon('play')));
                const playBtn = h('button', { type: 'button', class: 'pl-btn', 'aria-label': 'Play' }, icon('play'));
                const time = h('span', { class: 'pl-time' }, '0:00');
                const seek = h('input', { type: 'range', class: 'pl-seek', min: 0, max: 0, step: 'any', value: 0, 'aria-label': 'Seek' });
                const rateBtn = h(
                    'button',
                    { type: 'button', class: 'pl-btn pl-rate', 'aria-label': 'Playback speed 1×', 'aria-haspopup': 'menu', 'aria-expanded': 'false' },
                    '1×'
                );
                const rateItems = RATES.map(r =>
                    h(
                        'button',
                        { type: 'button', role: 'menuitemradio', 'aria-checked': String(r === 1), tabindex: '-1', onclick: () => setRate(r) },
                        `${r}×`
                    )
                );
                const rateMenu = h('div', { class: 'pl-menu', role: 'menu', 'aria-label': 'Playback speed', hidden: true }, rateItems);
                const rateWrap = h('div', { class: 'pl-rate-wrap' }, rateBtn, rateMenu);
                // Safari moves focus to the player on mouse press, which would close the menu before the click lands
                rateWrap.addEventListener('mousedown', e => e.preventDefault());
                const fsBtn = h('button', { type: 'button', class: 'pl-btn', 'aria-label': 'Full screen' }, icon('expand'));
                const root = h(
                    'div',
                    { class: 'player', role: 'group', 'aria-label': `Recording ${name}`, tabindex: '0' },
                    screen,
                    h('div', { class: 'pl-bar' }, playBtn, time, seek, rateWrap, fsBtn)
                );

                const dur = () => (Number.isFinite(video.duration) ? video.duration : 0);
                const toggle = () => (video.paused ? video.play().catch(() => {}) : video.pause());
                const seekBy = d => {
                    video.currentTime = Math.min(dur(), Math.max(0, video.currentTime + d));
                    paintTime();
                };

                function paintTime() {
                    const d = dur();
                    const t = video.currentTime;
                    time.textContent = d ? `${fmtClock(t)} / ${fmtClock(d)}` : fmtClock(t);
                    seek.max = d;
                    seek.value = t;
                    seek.style.setProperty('--f', d ? Math.min(1, t / d) : 0);
                }

                function paintState() {
                    const playing = !video.paused;
                    root.classList.toggle('is-playing', playing);
                    playBtn.replaceChildren(icon(playing ? 'pause' : 'play'));
                    playBtn.setAttribute('aria-label', playing ? 'Pause' : 'Play');
                }

                function openRates() {
                    rateMenu.hidden = false;
                    rateBtn.setAttribute('aria-expanded', 'true');
                    rateItems[RATES.indexOf(video.playbackRate)]?.focus();
                }

                function closeRates(refocus) {
                    if (rateMenu.hidden) return;
                    rateMenu.hidden = true;
                    rateBtn.setAttribute('aria-expanded', 'false');
                    if (refocus) rateBtn.focus();
                }

                function setRate(r) {
                    video.playbackRate = r;
                    rateBtn.textContent = `${r}×`;
                    rateBtn.setAttribute('aria-label', `Playback speed ${r}×`);
                    rateItems.forEach((b, i) => b.setAttribute('aria-checked', String(RATES[i] === r)));
                    closeRates(true);
                }

                // timeupdate only fires a few times a second; follow frames so the bar moves smoothly
                function tick() {
                    paintTime();
                    if (!video.paused && root.isConnected) requestAnimationFrame(tick);
                }

                function toggleFullscreen() {
                    if (document.fullscreenElement === root) document.exitFullscreen();
                    else if (root.requestFullscreen) root.requestFullscreen().catch(() => {});
                    else video.webkitEnterFullscreen?.();
                }

                video.addEventListener('play', () => {
                    paintState();
                    tick();
                });
                video.addEventListener('pause', paintState);
                ['loadedmetadata', 'durationchange', 'timeupdate'].forEach(ev => video.addEventListener(ev, paintTime));
                video.addEventListener('error', () => root.replaceWith(h('div', { class: 'shot-missing' }, 'Recording could not be played')));

                screen.addEventListener('click', toggle);
                screen.addEventListener('dblclick', toggleFullscreen);
                playBtn.addEventListener('click', toggle);
                fsBtn.hidden = !(root.requestFullscreen || video.webkitEnterFullscreen);
                fsBtn.addEventListener('click', toggleFullscreen);
                root.addEventListener('fullscreenchange', () => {
                    const full = document.fullscreenElement === root;
                    fsBtn.replaceChildren(icon(full ? 'shrink' : 'expand'));
                    fsBtn.setAttribute('aria-label', full ? 'Exit full screen' : 'Full screen');
                });
                rateBtn.addEventListener('click', () => (rateMenu.hidden ? openRates() : closeRates(true)));
                // Focus sits on an option while open, so leaving the menu in any way closes it
                rateWrap.addEventListener('focusout', e => {
                    if (!rateWrap.contains(e.relatedTarget)) closeRates(false);
                });
                rateMenu.addEventListener('keydown', e => {
                    const i = rateItems.indexOf(document.activeElement);
                    const to = { ArrowDown: i + 1, ArrowUp: i - 1, Home: 0, End: rateItems.length - 1 }[e.key];
                    if (to != null) rateItems[wrapIndex(to, rateItems.length)].focus();
                    else if (e.key === 'Escape' || e.key === 'Tab') closeRates(true);
                    else return;
                    if (e.key !== 'Tab') e.preventDefault();
                    e.stopPropagation();
                });

                // Hold playback while the thumb is dragged so it doesn't fight the playhead
                let resume = false;
                seek.addEventListener('pointerdown', () => {
                    resume = !video.paused;
                    video.pause();
                });
                seek.addEventListener('input', () => {
                    video.currentTime = +seek.value;
                    paintTime();
                });
                seek.addEventListener('change', () => {
                    if (resume) video.play().catch(() => {});
                    resume = false;
                });

                root.addEventListener('keydown', e => {
                    if (e.metaKey || e.ctrlKey || e.altKey) return;
                    const onButton = e.target.tagName === 'BUTTON';
                    if (e.key === 'k' || (e.key === ' ' && !onButton)) toggle();
                    else if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') seekBy(e.key === 'ArrowLeft' ? -1 : 1);
                    else if (e.key === 'f' && !fsBtn.hidden) toggleFullscreen();
                    else return;
                    // Keep 'f' and the arrows from reaching the report's own shortcuts
                    e.preventDefault();
                    e.stopPropagation();
                });

                return root;
            }

            function renderVideos(arts) {
                const videos = arts.filter(isVideo);
                if (!videos.length) return null;
                return h(
                    'div',
                    { class: 'videos' },
                    videos.map(a => {
                        const src = mediaSrc(a, 'video');
                        return h(
                            'div',
                            { class: 'video' },
                            src ? createPlayer(src, a.name || 'recording') : h('div', { class: 'shot-missing' }, 'Video not available'),
                            h('span', { class: 'cap', 'data-tip': a.path || '' }, a.name || 'recording')
                        );
                    })
                );
            }

            function renderFiles(arts) {
                const files = arts.filter(a => !isImage(a) && !isVideo(a) && !isHttp(a));
                if (!files.length) return null;
                return h(
                    'div',
                    { class: 'files' },
                    files.map(a => {
                        const name = a.name || a.path || 'attachment';
                        if (a.content) {
                            return h(
                                'details',
                                { class: 'dz file' },
                                h(
                                    'summary',
                                    null,
                                    h('span', null, name),
                                    a.type ? h('span', { class: 'ftype' }, a.type) : null,
                                    h('span', { class: 'muted' }, fmtBytes(a.size))
                                ),
                                h('pre', { class: 'body' }, prettyBody(a.content))
                            );
                        }
                        const href = safeUrl(a.path);
                        return h(
                            'div',
                            { class: 'file-row' },
                            href ? h('a', { href, download: a.name || true }, name) : h('span', null, name),
                            a.type ? h('span', { class: 'muted mono' }, a.type) : null,
                            h('span', { class: 'muted mono' }, fmtBytes(a.size))
                        );
                    })
                );
            }

            function renderKv(tc, suite) {
                const rows = [];
                if (tc.bddType) rows.push(['Feature', tc.featureName || suite.name], ['Type', tc.bddType.replace(/_/g, ' ')]);
                if (tc.className) rows.push(['Class', tc.className]);
                if (tc.methodName) rows.push(['Method', tc.methodName]);
                if (tc.startTime) rows.push(['Started', new Date(tc.startTime).toLocaleString()]);
                rows.push(['Duration', fmtDur(tc.duration)]);
                if (tc.retryCount) rows.push(['Retries', String(tc.retryCount)]);
                for (const m of tc.metrics || []) rows.push([m.name, `${m.value} ${m.unit || ''}`.trim()]);
                return h(
                    'dl',
                    { class: 'kv' },
                    rows.flatMap(([k, v]) => [h('dt', null, k), h('dd', null, v)])
                );
            }

            // ---------- Overview / empty ----------
            function renderOverview() {
                const pane = $('#detail');
                if (!tests.length) {
                    pane.replaceChildren(
                        h(
                            'div',
                            { class: 'empty' },
                            h('h2', null, 'No test results were recorded'),
                            h(
                                'p',
                                null,
                                'PulseReport finished without receiving any results. Check that an adapter is registered, for example ',
                                h('code', null, 'TestNGAdapter'),
                                ' as a listener in ',
                                h('code', null, 'testng.xml'),
                                ', and that ',
                                h('code', null, 'reporter.output.formats'),
                                ' includes ',
                                h('code', null, 'html'),
                                '.'
                            )
                        )
                    );
                    return;
                }
                const slow = [...tests].sort((a, b) => (b.tc.duration || 0) - (a.tc.duration || 0)).slice(0, 5);
                pane.replaceChildren(
                    h(
                        'div',
                        { class: 'detail enter' },
                        h('h2', { class: 'ov-title' }, 'Slowest tests'),
                        h(
                            'p',
                            { class: 'ov-sub' },
                            `Nothing failed in this run. ${slow.length === 1 ? 'This test took' : `These ${slow.length} tests took`} the longest.`
                        ),
                        h(
                            'ol',
                            { class: 'slow' },
                            slow.map(t =>
                                h(
                                    'li',
                                    null,
                                    h(
                                        'button',
                                        { type: 'button', class: 'item', onclick: () => select(t.tc.id, { reveal: true }) },
                                        glyph(t.tc.status),
                                        h('span', { class: 'name' }, t.tc.name, h('span', { class: 'suite' }, t.suite.name)),
                                        h('span', { class: 'dur' }, fmtDur(t.tc.duration))
                                    )
                                )
                            )
                        )
                    )
                );
            }

            // ---------- Lightbox: the same viewer, expanded ----------
            const lightbox = $('#lightbox');
            let modalViewer = null;

            function showLightbox(items, i) {
                modalViewer = createViewer(items, { start: i, expanded: true });
                lightbox.replaceChildren(modalViewer.root);
                if (!lightbox.open) lightbox.showModal();
                modalViewer.root.focus({ preventScroll: true });
            }

            // Idempotent: runs for our own close paths and the native dialog close event
            function finishLightbox() {
                const v = modalViewer;
                if (!v) return;
                modalViewer = null;
                lightbox.replaceChildren();
                if (v.items === gallery) {
                    setHash();
                    inlineViewer?.show(v.index, { animate: false });
                }
            }

            function closeLightbox() {
                if (lightbox.open) lightbox.close();
                finishLightbox();
            }

            lightbox.addEventListener('close', finishLightbox);
            lightbox.addEventListener('cancel', e => {
                e.preventDefault();
                closeLightbox();
            });
            // The dialog box is wider than the screenshot, so its empty areas behave like the backdrop
            const isEmptySpace = t => !t.closest('img, button, .car-live, .car-file');
            let downOnEmpty = false;
            lightbox.addEventListener('pointerdown', e => {
                downOnEmpty = isEmptySpace(e.target);
            });
            lightbox.addEventListener('click', e => {
                // Both ends must be empty so a pan or text selection that ends off the image doesn't close it
                if (downOnEmpty && isEmptySpace(e.target)) closeLightbox();
            });

            // ---------- Theme: follows the system until the user picks one ----------
            const themeBtn = $('#theme-btn');
            const systemDark = matchMedia('(prefers-color-scheme: dark)');
            const currentTheme = () => {
                const t = document.documentElement.dataset.theme;
                return t === 'light' || t === 'dark' ? t : systemDark.matches ? 'dark' : 'light';
            };

            function syncThemeButton() {
                const dark = currentTheme() === 'dark';
                const label = `Switch to ${dark ? 'light' : 'dark'} theme`;
                themeBtn.replaceChildren(icon(dark ? 'sun' : 'moon'));
                themeBtn.dataset.tip = label;
                themeBtn.setAttribute('aria-label', label);
                refreshTip(themeBtn);
            }

            function setTheme(theme) {
                const apply = () => {
                    document.documentElement.dataset.theme = theme;
                    syncThemeButton();
                };
                try {
                    if (theme === 'auto') localStorage.removeItem('pulse-report-theme');
                    else localStorage.setItem('pulse-report-theme', theme);
                } catch {
                    /* storage unavailable */
                }
                if (
                    document.startViewTransition &&
                    document.visibilityState === 'visible' &&
                    !matchMedia('(prefers-reduced-motion: reduce)').matches
                ) {
                    document.startViewTransition(apply);
                } else apply();
            }

            themeBtn.addEventListener('click', () => {
                const next = currentTheme() === 'dark' ? 'light' : 'dark';
                setTheme(next === (systemDark.matches ? 'dark' : 'light') ? 'auto' : next);
            });

            systemDark.addEventListener('change', () => setTheme('auto'));
            syncThemeButton();

            // ---------- Keyboard ----------
            function move(dir) {
                const visible = groups
                    .filter(g => !g.det.hidden)
                    .flatMap(g => g.cases.filter(tc => matches(byId.get(tc.id))).map(tc => ({ g, id: tc.id })));
                if (!visible.length) return;
                let i = visible.findIndex(v => v.id === state.selected);
                i = i < 0 ? (dir > 0 ? 0 : visible.length - 1) : Math.min(visible.length - 1, Math.max(0, i + dir));
                const { g, id } = visible[i];
                openGroup(g);
                select(id);
                const it = itemEls.get(id);
                if (document.activeElement?.closest('.tree')) it.btn.focus();
                it.btn.scrollIntoView({ block: 'nearest' });
            }

            document.addEventListener('keydown', e => {
                if (lightbox.open) {
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        closeLightbox();
                    } else if (!e.defaultPrevented) modalViewer?.handleKey(e);
                    return;
                }
                if (e.metaKey || e.ctrlKey || e.altKey) return;
                const typing = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement?.tagName || '');
                if (typing) {
                    if (e.key === 'Escape' && document.activeElement === search) {
                        if (search.value) {
                            search.value = '';
                            search.dispatchEvent(new Event('input'));
                        } else search.blur();
                    }
                    return;
                }
                if (e.key === '/') {
                    e.preventDefault();
                    search.focus();
                    search.select();
                } else if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && !document.activeElement?.closest('.player')) {
                    e.preventDefault();
                    move(e.key === 'ArrowDown' ? 1 : -1);
                } else if (e.key === 'f' && totals.FAILED) {
                    state.status = state.status === 'FAILED' ? 'all' : 'FAILED';
                    applyFilter();
                }
            });

            window.addEventListener('hashchange', () => {
                const id = idFromHash();
                const shot = shotFromHash();
                if (id && id !== state.selected) select(id, { reveal: true });
                openShotFromHash(shot);
            });

            // ---------- Print: every failure with its full trace ----------
            window.addEventListener('beforeprint', () => {
                const failed = timeline.filter(t => t.tc.status === 'FAILED');
                $('#print-failures').replaceChildren(
                    ...(failed.length ? [h('h2', null, 'Failures')] : []),
                    ...failed.map(t =>
                        h(
                            'section',
                            { class: 'pf' },
                            h('h3', null, t.tc.name),
                            h('p', { class: 'muted mono' }, `${locationOf(t.tc, t.suite)}  ${fmtDur(t.tc.duration)}`),
                            renderFailure(t.tc, { open: true })
                        )
                    )
                );
            });

            // ---------- Boot ----------
            renderTopbar();
            renderVerdict();
            renderChips();
            renderTree();
            applyFilter();
            if (!tests.length) app.classList.add('is-empty');
            const fromHash = idFromHash();
            const shot = shotFromHash();
            const initial = fromHash && byId.has(fromHash) ? fromHash : timeline.find(t => t.tc.status === 'FAILED')?.tc.id;
            if (initial) select(initial, { reveal: true, initial: !fromHash });
            else renderOverview();
            if (fromHash) openShotFromHash(shot);
        })();
    </script>
    </#noparse>
</body>
</html>
