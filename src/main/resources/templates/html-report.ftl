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
            color-scheme: light;
        }

        @media screen {
            :root[data-theme='dark'] {
                --surface: #121416;
                --raised: #181b1e;
                --sunken: #0d0f11;
                --border: #2a2f34;
                --border-strong: #3a4046;
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

        .step.is-fail {
            background: var(--fail-bg);
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
            object-fit: cover;
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

        .videos {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 14px;
        }

        .artifact-video {
            display: block;
            width: 100%;
            max-height: 480px;
            border-radius: var(--radius);
            background: var(--sunken);
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
            max-width: min(1200px, 94vw);
            max-height: 92dvh;
            padding: 0;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--raised);
            color: var(--ink);
        }

        .lightbox::backdrop {
            background: rgb(13 15 17 / 0.74);
        }

        .lb-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            padding: 8px 8px 8px 14px;
            border-bottom: 1px solid var(--border);
            font: 12px var(--font-mono);
        }

        .lightbox img {
            display: block;
            max-width: 100%;
            max-height: calc(92dvh - 48px);
            margin: auto;
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

    <dialog class="lightbox" id="lightbox" aria-label="Screenshot">
        <div class="lb-bar">
            <span id="lb-name"></span>
            <button type="button" class="btn" id="lb-close">Close</button>
        </div>
        <img id="lb-img" alt="">
    </dialog>

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
    </template>

    <script type="application/json" id="pr-data">${runJson?no_esc}</script>
    <#noparse>
    <script>
        (() => {
            'use strict';
            const RUN = JSON.parse(document.getElementById('pr-data').textContent);
            const $ = sel => document.querySelector(sel);

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
                if (/^data:image\/(png|jpe?g|gif|webp|svg\+xml)[;,]/i.test(s)) return s;
                if (/^(https?|file):/i.test(s)) return s;
                return /^[a-z][a-z0-9+.-]*:/i.test(s) ? null : s;
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
                const max = Math.max(1, ...timeline.map(t => t.tc.duration || 0));
                const track = h('div', {
                    class: 'pulse-track',
                    role: 'img',
                    'aria-label': `Run timeline: ${totals.all} ${totals.all === 1 ? 'test' : 'tests'} in execution order, bar height shows duration. ${totals.FAILED} failed, ${totals.SKIPPED} skipped.`
                });
                if (timeline.length > 160) track.style.setProperty('--bar-gap', '1px');
                for (const t of timeline) {
                    const d = t.tc.duration || 0;
                    const bar = h('div', {
                        class: 'bar',
                        'data-s': st(t.tc.status).key,
                        'data-id': t.tc.id,
                        'data-tip': `${t.tc.name}\n${st(t.tc.status).label}, ${fmtDur(d)}`
                    });
                    bar.style.height = `${14 + 86 * Math.sqrt(d / max)}%`;
                    barEls.set(t.tc.id, bar);
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

            function renderTree() {
                const nodes = [];
                for (const suite of RUN.suites || []) {
                    const cases = suite.testCases || [];
                    if (!cases.length) continue;
                    const failed = cases.filter(c => c.status === 'FAILED').length;
                    const ul = h('ul', { class: 'items' });
                    const det = h(
                        'details',
                        { class: 'group dz', open: failed > 0 },
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
                    for (const tc of cases) {
                        const btn = h(
                            'button',
                            { type: 'button', class: 'item', 'data-id': tc.id },
                            glyph(tc.status),
                            h('span', { class: 'name' }, srStatus(tc.status), tc.name),
                            h('span', { class: 'dur' }, fmtDur(tc.duration))
                        );
                        btn.addEventListener('click', () => select(tc.id));
                        const li = h('li', null, btn);
                        ul.append(li);
                        itemEls.set(tc.id, { li, btn, det, entry: byId.get(tc.id) });
                    }
                    groups.push(det);
                    nodes.push(det);
                }
                emptyEl = h('div', { class: 'tree-empty', hidden: true });
                $('#tree').replaceChildren(...nodes, emptyEl);
            }

            function matches(t) {
                if (state.status !== 'all' && t.tc.status !== state.status) return false;
                return !state.q || t.hay.includes(state.q);
            }

            function applyFilter() {
                const filtering = state.status !== 'all' || state.q;
                const hits = new Set();
                let any = 0;
                for (const it of itemEls.values()) {
                    const ok = matches(it.entry);
                    it.li.hidden = !ok;
                    if (ok) {
                        any++;
                        hits.add(it.det);
                    }
                }
                for (const det of groups) {
                    det.hidden = !hits.has(det);
                    if (filtering && hits.has(det)) det.open = true;
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

            search.addEventListener('input', () => {
                state.rawQ = search.value.trim();
                state.q = state.rawQ.toLowerCase();
                applyFilter();
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
                const it = itemEls.get(id);
                if (it) {
                    it.btn.setAttribute('aria-current', 'true');
                    if (opts.reveal) {
                        if (it.li.hidden) clearFilters();
                        it.det.open = true;
                        it.btn.scrollIntoView({ block: 'nearest' });
                    }
                }
                barEls.get(id)?.setAttribute('aria-current', 'true');
                renderDetail(entry);
                if (!opts.initial) {
                    try {
                        history.replaceState(null, '', `#t=${encodeURIComponent(id)}`);
                    } catch {
                        /* file:// in some browsers */
                    }
                    app.dataset.view = 'detail';
                    if (matchMedia('(max-width: 767px)').matches) $('#detail').scrollIntoView();
                }
            }

            function idFromHash() {
                const m = location.hash.match(/^#t=(.+)$/);
                if (!m) return null;
                try {
                    return decodeURIComponent(m[1]);
                } catch {
                    return null;
                }
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
                const steps = renderSteps(tc);
                if (steps)
                    d.append(
                        section('Steps', tc.featureDescription ? h('p', { class: 'desc' }, tc.featureDescription) : null, steps)
                    );
                const arts = tc.artifacts || [];
                const exchanges = pairExchanges(arts);
                if (exchanges.length)
                    d.append(section(exchanges.length > 1 ? 'HTTP exchanges' : 'HTTP exchange', exchanges.map(exchangeEl)));
                const shots = renderShots(arts);
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
                    h('span', { class: 'dur' }, s.duration ? fmtDur(s.duration) : '')
                );
                const arts = s.artifacts || [];
                const extra = [];
                if (s.errorMessage && s.errorMessage !== ctx.testError) extra.push(h('pre', { class: 'step-err' }, s.errorMessage));
                if (s.dataTable?.length) extra.push(dataTable(s.dataTable));
                if (s.docString) extra.push(h('pre', { class: 'body' }, s.docString));
                for (const pair of pairExchanges(arts)) {
                    extra.push(h('details', { class: 'dz xwrap' }, h('summary', null, exchangeTitle(pair)), exchangeEl(pair)));
                }
                extra.push(renderShots(arts), renderVideos(arts), renderFiles(arts));
                const present = extra.filter(Boolean);
                if (present.length) li.append(h('div', { class: 'step-extra' }, present));
                return li;
            }

            function dataTable(rows) {
                const [head, ...body] = rows;
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
                                head.map(c => h('th', { scope: 'col' }, c))
                            )
                        ),
                        h(
                            'tbody',
                            null,
                            body.map(r =>
                                h(
                                    'tr',
                                    null,
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
                if (res) title += ` → ${statusCode(res) || res.line}`;
                else if (req) title += ' → no response';
                return title;
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
                    const cls = code >= 400 ? 's-fail' : code >= 200 && code < 300 ? 's-pass' : '';
                    line = h('div', { class: `xline xstatus ${cls}` }, code ? `${code} ${REASONS[code] || ''}`.trim() : p.line);
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
                        btn.addEventListener('click', () => openLightbox(src, name));
                        return btn;
                    })
                );
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
                            src
                                ? h('video', { class: 'artifact-video', src, controls: true, preload: 'metadata' })
                                : h('div', { class: 'shot-missing' }, 'Video not available'),
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

            // ---------- Lightbox ----------
            const lightbox = $('#lightbox');

            function openLightbox(src, name) {
                const img = $('#lb-img');
                img.src = src;
                img.alt = name || 'Screenshot';
                $('#lb-name').textContent = name || '';
                lightbox.showModal();
            }

            lightbox.addEventListener('click', e => {
                if (e.target === lightbox) lightbox.close();
            });

            $('#lb-close').addEventListener('click', () => lightbox.close());

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
                const visible = [...itemEls.values()].filter(it => !it.li.hidden && !it.det.hidden);
                if (!visible.length) return;
                let i = visible.findIndex(it => it.btn.dataset.id === state.selected);
                i = i < 0 ? (dir > 0 ? 0 : visible.length - 1) : Math.min(visible.length - 1, Math.max(0, i + dir));
                const it = visible[i];
                it.det.open = true;
                select(it.btn.dataset.id);
                if (document.activeElement?.closest('.tree')) it.btn.focus();
                it.btn.scrollIntoView({ block: 'nearest' });
            }

            document.addEventListener('keydown', e => {
                if (lightbox.open) {
                    if (e.key === 'Escape') {
                        e.preventDefault();
                        lightbox.close();
                    }
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
                } else if ((e.key === 'ArrowDown' || e.key === 'ArrowUp') && document.activeElement?.tagName !== 'VIDEO') {
                    e.preventDefault();
                    move(e.key === 'ArrowDown' ? 1 : -1);
                } else if (e.key === 'f' && totals.FAILED) {
                    state.status = state.status === 'FAILED' ? 'all' : 'FAILED';
                    applyFilter();
                }
            });

            window.addEventListener('hashchange', () => {
                const id = idFromHash();
                if (id && id !== state.selected) select(id, { reveal: true });
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
            const initial = fromHash && byId.has(fromHash) ? fromHash : timeline.find(t => t.tc.status === 'FAILED')?.tc.id;
            if (initial) select(initial, { reveal: true, initial: !fromHash });
            else renderOverview();
        })();
    </script>
    </#noparse>
</body>
</html>
