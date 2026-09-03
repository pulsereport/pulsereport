<!DOCTYPE html>
<html lang="en">
<head>
    <meta name="color-scheme" content="light dark">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PulseReport - ${testRun.name?html}</title>
    <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,%3Csvg xmlns='http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg' viewBox='0 0 48 48'%3E%3Crect x='2' y='2' width='44' height='44' rx='12' fill='%23F8F9FA' stroke='%23DEE2E6' stroke-width='1.5'/%3E%3Cpath d='M12 24H19L22.5 19L26.5 31L31 16L35 24H38' stroke='%230F8B8D' stroke-width='2.6' fill='none' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E">
    
    <style>
        :root {
            /* ── Primitives ── */
            --color-teal-600: #0f8b8d;
            --color-teal-400: #58b7b8;
            --color-gray-0: #ffffff;
            --color-gray-100: #f8f9fa;
            --color-gray-200: #e9ecef;
            --color-gray-300: #dee2e6;
            --color-gray-500: #adb5bd;
            --color-gray-600: #6c757d;
            --color-gray-700: #495057;
            --color-gray-900: #212529;
            --color-green-600: #198754;
            --color-red-500: #dc3545;
            --color-red-100: #f8d7da;
            --color-red-200: #f1aeb5;
            --color-amber-400: #ffc107;
            --color-cyan-400: #0dcaf0;
            --color-cyan-100: #cff4fc;
            --color-cyan-200: #9eeaf9;
            --color-violet-600: #7c3aed;
            --color-orange-600: #ea580c;
            /* ── Surfaces ── */
            --bg: var(--color-gray-100);
            --surface: var(--color-gray-0);
            --surface-raised: var(--color-gray-200);
            --surface-overlay: var(--color-gray-100);
            --border: var(--color-gray-300);
            --border-light: var(--color-gray-200);
            /* ── Text ── */
            --text-primary: var(--color-gray-900);
            --text-secondary: var(--color-gray-600);
            --text-muted: var(--color-gray-500);
            /* ── Accent ── */
            --accent: var(--color-teal-600);
            --accent-light: var(--color-gray-200);
            /* ── Logo ── */
            --logo-tile-fill: var(--color-gray-100);
            --logo-tile-stroke: var(--color-gray-300);
            --logo-pulse-stroke: var(--accent);
            /* ── Status ── */
            --green: var(--color-green-600);
            --red: var(--color-red-500);
            --red-bg: var(--color-red-100);
            --red-border: var(--color-red-200);
            --amber: var(--color-amber-400);
            --blue: var(--color-cyan-400);
            --blue-bg: var(--color-cyan-100);
            --blue-border: var(--color-cyan-200);
            --violet: var(--color-violet-600);
            --orange: var(--color-orange-600);
            --not-run: var(--color-gray-600);
            /* ── Layout ── */
            --radius-sm: 6px;
            --radius: 10px;
            /* ── Typography ── */
            --font-brand: "Sora", "Segoe UI", sans-serif;
            --font: "Source Sans 3", "Segoe UI", sans-serif;
            --font-mono: 'SF Mono', 'Fira Code', 'Cascadia Code', 'Courier New', monospace;
            color-scheme: light dark;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        html { background: var(--bg); }

        body {
            font-family: var(--font);
            background: var(--bg);
            color: var(--text-primary);
            line-height: 1.5;
            font-size: 14px;
            -webkit-font-smoothing: antialiased;
            overflow-x: hidden; /* fallback for Safari < 16 */
            overflow-x: clip;   /* clips without creating a scroll container */
        }

        .container {
            max-width: 1100px;
            margin: 0 auto;
            padding: 32px 24px;
        }

        .header-top {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
            margin-bottom: 22px;
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-left: auto;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .brand-icon {
            width: 48px;
            height: 48px;
            flex-shrink: 0;
        }

        .brand-wordmark {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .brand-title {
            font-family: var(--font-brand);
            font-size: 1.35rem;
            font-weight: 700;
            letter-spacing: -0.03em;
            color: var(--text-primary);
            line-height: 1;
        }

        .brand-subtitle {
            color: var(--text-secondary);
            font-size: 0.82rem;
            font-weight: 600;
            letter-spacing: 0.02em;
        }

        .report-hero {
            position: relative;
            overflow: hidden;
            border: 1px solid var(--border);
            border-radius: 18px;
            background: var(--surface);
            padding: 26px 28px;
            margin-bottom: 28px;
        }

        .report-hero::before {
            content: "";
            position: absolute;
            inset: 0;
            pointer-events: none;
        }

        .report-hero-body {
            position: relative;
            display: flex;
            align-items: stretch;
            justify-content: space-between;
            gap: 20px;
            flex-wrap: nowrap;
        }

        .report-hero-content {
            min-width: 0;
            flex: 1 1 620px;
        }

        .report-hero-side {
            display: flex;
            flex-direction: column;
            align-items: flex-end;
            gap: 18px;
            flex: 0 1 280px;
        }

        .report-hero-kicker {
            color: var(--text-secondary);
            font-size: 0.78rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            margin-bottom: 10px;
        }

        .report-hero-title {
            font-family: var(--font-brand);
            font-size: clamp(2.1rem, 4vw, 2.85rem);
            font-weight: 700;
            letter-spacing: -0.04em;
            line-height: 0.98;
            color: var(--text-primary);
        }

        .report-hero-subtitle {
            max-width: 60ch;
            margin-top: 12px;
            color: var(--text-secondary);
            font-size: 0.98rem;
            line-height: 1.45;
        }

        .report-hero-meta {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 22px;
        }

        .report-meta-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            min-height: 36px;
            padding: 8px 13px;
            border-radius: 999px;
            background: rgba(255, 255, 255, 0.84);
            border: 1px solid var(--border);
            color: var(--text-primary);
            font-size: 0.82rem;
            font-weight: 600;
        }

        .hero-status-pill {
            height: 37px;
            border-color: var(--border);
        }

        .hero-status-pill.passed {
            color: var(--green);
        }

        .hero-status-pill.failed {
            color: var(--red);
        }

        .hero-status-pill.skipped,
        .hero-status-pill.flaky {
            color: var(--amber);
        }

        .hero-status-pill.not-run {
            color: var(--not-run);
        }

        .report-meta-pill-icon {
            position: relative;
            gap: 8px;
        }

        .report-meta-icon {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 18px;
            height: 18px;
            flex-shrink: 0;
            color: var(--accent);
        }

        .report-meta-icon svg {
            width: 16px;
            height: 16px;
            stroke: currentColor;
        }

        .report-meta-value {
            display: inline-flex;
            align-items: center;
        }

        .sr-only {
            position: absolute;
            width: 1px;
            height: 1px;
            padding: 0;
            margin: -1px;
            overflow: hidden;
            clip: rect(0, 0, 0, 0);
            white-space: nowrap;
            border: 0;
        }

        .report-hero-summary {
            width: min(100%, 280px);
            padding: 16px 18px;
            border-radius: 16px;
            border: 1px solid var(--border);
            background: rgba(255, 255, 255, 0.76);
            backdrop-filter: blur(10px);
        }

        .report-hero-summary-label {
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            color: var(--text-muted);
            margin-bottom: 12px;
        }

        .report-hero-summary-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 12px;
        }

        .report-hero-stat {
            min-width: 0;
        }

        .report-hero-stat-label {
            display: block;
            font-size: 0.72rem;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.04em;
            margin-bottom: 4px;
        }

        .report-hero-stat-value {
            display: block;
            font-family: var(--font-brand);
            font-size: 1.55rem;
            font-weight: 700;
            letter-spacing: -0.03em;
            color: var(--text-primary);
            line-height: 1;
        }

        .report-hero-breakdown {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 14px;
            padding-top: 14px;
            border-top: 1px solid var(--border);
        }

        .report-hero-breakdown-item {
            display: inline-flex;
            align-items: center;
            min-height: 28px;
            padding: 4px 10px;
            border: 1px solid var(--border);
            border-radius: 999px;
            background: var(--surface);
            font-size: 0.78rem;
            font-weight: 600;
            color: var(--text-secondary);
        }

        .report-hero-breakdown-item.pass {
            color: var(--green);
        }

        .report-hero-breakdown-item.fail {
            color: var(--red);
        }

        .report-hero-breakdown-item.skip {
            color: var(--amber);
        }

        .report-meta-pill, .report-hero-summary {
            background: var(--bg);
        }

        /* ── Slim Navbar (fixed, slides in once the hero scrolls away) ── */
        .report-navbar {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            z-index: 300;
            background: var(--surface);
            border-bottom: 1px solid var(--border);
            transform: translateY(-100%);
            visibility: hidden;
        }

        .report-navbar-visible {
            transform: translateY(0);
            visibility: visible;
        }

        .report-navbar-inner {
            padding: 10px 28px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
        }

        .report-navbar-stats {
            display: flex;
            align-items: center;
            gap: 16px;
            min-width: 0;
            margin-left: 20px;
        }

        .report-navbar .brand-icon {
            width: 32px;
            height: 32px;
        }

        .report-navbar .brand-title {
            font-size: 1.05rem;
        }

        .report-navbar .hero-status-pill {
            padding: 5px 12px;
            font-size: 0.75rem;
            min-height: 28px;
        }

        @media (prefers-reduced-motion: no-preference) {
            .report-navbar {
                transition: transform 0.3s cubic-bezier(.4,0,.2,1), visibility 0.3s;
            }
        }

        @media print {
            .report-navbar { display: none; }

            .bdd-table-scroll,
            .stack-trace,
            .bdd-step-stack,
            .artifact-code {
                overflow: visible;
            }
        }

        .compact-stat {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 1px;
        }

        .compact-stat-label {
            font-size: 0.62rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            color: var(--text-muted);
            line-height: 1;
        }

        .compact-stat-value {
            font-family: var(--font-brand);
            font-size: 1.05rem;
            font-weight: 700;
            letter-spacing: -0.02em;
            color: var(--text-primary);
            line-height: 1.1;
        }

        /* ── Test Suite ── */
        .test-suite {
            background: var(--surface);
            margin-bottom: 12px;
            border-radius: var(--radius);
            border: 1px solid var(--border);
            overflow: hidden;
            scroll-margin-top: 90px;
        }

        .test-suite-header {
            padding: 14px 18px;
            cursor: pointer;
            user-select: none;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            border-left: 2px solid var(--border);
            border-top-left-radius: 9px;
            transition: background 0.15s;
        }

        .test-suite-header.collapsed { border-bottom-left-radius: 9px; }
        .test-suite-header:hover { background: var(--bg); }
        .test-suite-header.passed { border-left-color: var(--green); }
        .test-suite-header.failed { border-left-color: var(--red); }

        .test-suite-header-content {
            flex: 1 1 auto;
            min-width: 0;
        }

        .test-suite-header h2 {
            font-size: 0.95rem;
            font-weight: 600;
            color: var(--text-primary);
            letter-spacing: -0.01em;
        }

        .suite-class-path {
            font-size: 0.72rem;
            font-weight: 500;
            color: var(--text-secondary);
            font-family: var(--font-mono);
            letter-spacing: 0;
            margin-left: 6px;
            overflow-wrap: anywhere;
        }

        .test-suite-stats {
            display: block;
            margin-top: 3px;
            font-size: 0.78rem;
            color: var(--text-muted);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .test-suite-header:not(.collapsed) .test-suite-stats {
            white-space: normal;
            overflow: visible;
        }
        .test-suite-stats .tag-label {
            font-size: inherit;
        }
        .suite-stats-sep {
            color: var(--text-muted);
        }

        .test-suite-toggle {
            color: var(--text-muted);
        }

        .suite-stats-ratio,
        .suite-stats-duration,
        .test-suite-toggle {
            flex-shrink: 0;
            white-space: nowrap;
        }

        .test-suite-body { overflow: hidden; }
        .test-suite-body.collapsed { display: none; }

        /* ── Test Case ── */
        .test-case {
            border-top: 1px solid var(--border-light);
        }

        .test-case:first-child { border-top: 1px solid var(--border); }

        .test-case-header {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 11px 18px;
            transition: background 0.15s;
        }

        .test-case-header.expandable {
            cursor: pointer;
            user-select: none;
        }

        .test-case-header.expandable:hover { background: var(--bg); }

        .test-case-body {
            max-height: 0;
            overflow: hidden;
            position: relative;
            transition: max-height 0.15s ease-out;
        }

        .test-case-body.expanded { max-height: none; }

        .test-case-content {
            padding: 4px 18px 16px 18px;
            border-top: 1px solid var(--border-light);
        }

        .test-case-name {
            font-weight: 500;
            flex: 1;
            min-width: 0;
            font-size: 0.88rem;
            color: var(--text-primary);
            overflow: hidden;
            overflow-wrap: anywhere;
        }

        .test-case-method {
            display: block;
            font-size: 0.75rem;
            font-weight: 400;
            color: var(--text-secondary);
            font-family: monospace;
            margin-top: 1px;
        }

        .test-case-time {
            font-size: 0.78rem;
            color: var(--text-muted);
            font-variant-numeric: tabular-nums;
            flex-shrink: 0;
            white-space: nowrap;
        }

        .test-case-toggle {
            color: var(--text-muted);
            flex-shrink: 0;
        }

        /* ── Tag Labels ── */
        .test-case-tags {
            display: inline-block;
            max-width: 40ch;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            vertical-align: bottom;
            color: var(--text-muted);
        }

        .test-suite-header:not(.collapsed) .test-case-tags,
        .test-case-header.expanded .test-case-name .test-case-tags {
            display: inline;
            max-width: none;
            overflow: visible;
            white-space: normal;
            text-overflow: clip;
        }

        .tags-hidden .test-case-tags,
        .tags-hidden .test-case-tags + .suite-stats-sep {
            display: none !important;
        }

        .test-case-name .test-case-tags {
            margin-left: 6px;
        }

        .tag-label {
            font-size: 0.7rem;
            font-weight: 400;
            color: var(--text-muted);
            white-space: nowrap;
            margin-right: 4px;
        }

        /* ── Tag Filter Dropdown ── */
        .tag-filter-wrapper {
            position: relative;
        }


        .tag-filter-dropdown {
            display: none;
            position: absolute;
            top: calc(100% + 4px);
            right: 0;
            min-width: 200px;
            max-width: calc(100vw - 24px);
            max-height: 280px;
            overflow-y: auto;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            z-index: 200;
            padding: 6px 0;
        }

        .tag-filter-dropdown.open {
            display: block;
        }

        .tag-filter-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            font-size: 0.78rem;
            color: var(--text-primary);
            cursor: pointer;
            transition: background 0.1s;
        }

        .tag-filter-item:hover {
            background: var(--bg);
        }

        .tag-filter-item input[type="checkbox"] {
            accent-color: var(--accent);
        }

        .tag-filter-clear {
            display: block;
            width: 100%;
            padding: 6px 12px;
            font-size: 0.75rem;
            font-weight: 600;
            color: var(--accent);
            background: none;
            border: none;
            border-top: 1px solid var(--border);
            cursor: pointer;
            text-align: left;
            margin-top: 4px;
        }

        .tag-filter-clear:hover {
            background: var(--bg);
        }

        .test-case-details {
            font-size: 0.8rem;
            color: var(--text-muted);
            margin-bottom: 10px;
            font-family: var(--font-mono);
        }

        /* ── Class / file header inside suite ── */
        .class-file-header {
            display: flex;
            align-items: center;
            gap: 7px;
            padding: 7px 16px 6px;
            font-size: 0.74rem;
            font-family: var(--font-mono);
            color: var(--text-muted);
            background: var(--bg);
            border-bottom: 1px solid var(--border-light);
            user-select: text;
            overflow-wrap: anywhere;
        }
        .class-file-header svg { flex-shrink: 0; opacity: 0.6; }

        /* ── Status badges ── */
        .status-badge {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            flex-shrink: 0;
        }
        .status-badge.passed { background: var(--green); }
        .status-badge.failed { background: var(--red); }
        .status-badge.skipped { background: var(--amber); }
        .status-badge.not-run { background: var(--text-muted); }
        .status-badge.flaky { background: var(--amber); }

        /* ── Error ── */
        .error-message {
            background: var(--red-bg);
            border: 1px solid var(--red-border);
            border-radius: var(--radius-sm);
            padding: 12px 14px;
            margin-top: 12px;
            font-family: var(--font-mono);
            font-size: 0.8rem;
            overflow-wrap: anywhere;
        }

        .error-message-title {
            font-weight: 600;
            color: var(--red);
            margin-bottom: 6px;
        }

        .stack-trace {
            white-space: pre-wrap;
            overflow-wrap: anywhere;
            color: var(--text-secondary);
            max-height: 260px;
            overflow-y: auto;
            overflow-x: auto;
            font-size: 0.78rem;
        }

        /* ── Artifact / API call ── */
        .artifact-section { margin-top: 14px; }

        /* ── Metrics ── */
        .metrics-section { margin-top: 12px; }
        .metrics-section-title {
            font-size: 0.68rem;
            font-weight: 700;
            text-transform: uppercase;
            color: var(--text-muted);
            letter-spacing: 0.07em;
            margin-bottom: 8px;
        }
        .metrics-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .metric-chip {
            display: inline-flex;
            align-items: baseline;
            gap: 5px;
            background: var(--bg);
            border: 1px solid var(--border-light);
            border-radius: 20px;
            padding: 4px 12px;
            font-size: 0.8rem;
        }
        .metric-chip-name {
            color: var(--text-muted);
            font-size: 0.75rem;
        }
        .metric-chip-value {
            font-weight: 600;
            color: var(--text-primary);
            font-family: var(--font-mono);
        }
        .metric-chip-unit {
            color: var(--text-muted);
            font-size: 0.73rem;
        }

        /* ── Inline screenshots ── */
        .screenshot-wrap { padding: 8px 0 4px; }
        .screenshot-thumb {
            max-width: 100%;
            max-height: 260px;
            border-radius: 6px;
            border: 1px solid var(--border-light);
            cursor: zoom-in;
            display: block;
            transition: opacity 0.15s;
        }
        .screenshot-thumb:hover { opacity: 0.85; }
        .video-wrap { padding: 8px 0 4px; }
        .artifact-video {
            max-width: 100%;
            max-height: 360px;
            border-radius: 6px;
            border: 1px solid var(--border-light);
            display: block;
            background: #000;
        }
        /* Lightbox */
        #screenshot-lightbox {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.82);
            z-index: 9999;
            align-items: center;
            justify-content: center;
            cursor: zoom-out;
        }
        #screenshot-lightbox.open { display: flex; }
        #screenshot-lightbox img {
            max-width: 92vw;
            max-height: 92vh;
            border-radius: 8px;
        }

        .api-call-card {
            margin: 10px 0px;
            border-radius: var(--radius-sm);
            overflow: hidden;
        }

        .api-call-header {
            background: var(--bg);
            padding: 9px 13px;
            cursor: pointer;
            user-select: none;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .api-call-header:hover { background: var(--border-light); }

        .api-call-title {
            font-weight: 600;
            color: var(--text-primary);
            flex: 1;
            font-size: 0.83rem;
            font-family: var(--font-mono);
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .api-call-toggle {
            color: var(--text-muted);
        }

        .api-call-body {
            max-height: 0;
            overflow: hidden;
            position: relative;
            transition: max-height 0.15s ease-out;
        }

        .api-call-body.expanded {
            max-height: none;
            overflow: visible;
        }

        .artifact-item { border-top: 1px solid var(--border-light); }
        .artifact-item:first-child { border-top: none; }

        .artifact-header {
            background: var(--surface);
            padding: 8px 13px;
            cursor: pointer;
            user-select: none;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .artifact-header:hover { background: var(--bg); }

        .artifact-type {
            font-weight: 200;
            text-transform: uppercase;
            font-size: 0.7rem;
            letter-spacing: 0.06em;
            min-width: 80px;
        }

        .artifact-duration {
            color: var(--text-primary);
            font-size: 0.82rem;
            flex: 1;
            min-width: 0;
            font-family: var(--font-mono);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .toggle-icon {
            color: var(--text-muted);
        }

        .artifact-content {
            max-height: 0;
            overflow: hidden;
            position: relative;
            transition: max-height 0.15s ease-out;
        }

        .artifact-content.expanded {
            max-height: none;
            overflow: visible;
        }

        .artifact-code {
            background: var(--bg);
            color: var(--text-primary);
            border-top: 1px solid var(--border-light);
            padding: 14px 16px;
            margin: 0;
            font-family: var(--font-mono);
            font-size: 0.8rem;
            white-space: pre-wrap;
            word-wrap: break-word;
            overflow-wrap: anywhere;
            line-height: 1.7;
            max-width: 100%;
            max-height: 40vh;
            overflow-x: auto;
            overflow-y: auto;
        }

        .artifact-content a {
            display: block;
            padding: 12px 14px;
            color: var(--accent);
            text-decoration: none;
            font-weight: 500;
            font-size: 0.85rem;
        }

        .artifact-content a:hover { background: var(--bg); }

        /* ── BDD / Gherkin ── */
        
        .bdd-background-section { margin-bottom: 14px; }

        .bdd-background-title,
        .bdd-scenario-steps-title {
            font-size: 0.68rem;
            font-weight: 700;
            text-transform: uppercase;
            color: var(--text-muted);
            letter-spacing: 0.07em;
            margin-bottom: 6px;
            margin-top: 10px;
        }

        .bdd-steps-list { list-style: none; margin: 0; padding: 0; }

        .bdd-step {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 5px 8px;
            border-radius: var(--radius-sm);
            margin-bottom: 2px;
            font-size: 0.85rem;
        }

        .bdd-step:hover { background: var(--bg); }

        .bdd-step.has-artifacts { cursor: pointer; }
        .bdd-step.has-error { cursor: pointer; }

        .bdd-step-http-label {
            display: inline-flex;
            align-items: center;
            background: var(--blue-bg);
            color: var(--blue);
            border: 1px solid var(--blue-border);
            border-radius: 20px;
            font-size: 0.65rem;
            font-weight: 700;
            padding: 1px 7px;
            flex-shrink: 0;
            pointer-events: none;
            letter-spacing: 0.04em;
            text-transform: uppercase;
        }

        .bdd-step-http-chevron {
            display: inline-flex;
            align-items: center;
            margin-left: 2px;
        }

        .bdd-step-http-chevron .chevron-svg {
            width: 11px;
            height: 11px;
            transition: transform 0.15s;
        }

        .bdd-step-keyword {
            min-width: 48px;
            font-weight: 700;
            color: var(--accent);
            text-align: right;
            flex-shrink: 0;
            font-size: 0.83rem;
        }

        .bdd-step-name {
            flex: 1;
            min-width: 0;
            word-break: break-word;
            color: var(--text-primary);
        }

        .bdd-step-duration {
            flex-shrink: 0;
            color: var(--text-secondary);
            font-size: 0.8rem;
            margin-left: 8px;
        }

        .bdd-step-desc-row { list-style: none; }
        .step-description {
            margin: 0 0 4px 34px;
            padding: 0;
            color: var(--text-secondary);
            font-size: 0.84rem;
            line-height: 1.4;
            white-space: pre-wrap;
            word-break: break-word;
        }

        .bdd-step-status { flex-shrink: 0; display: inline-flex; align-items: center; }

        .bdd-step-icon {
            display: inline-flex;
            align-items: center;
            flex-shrink: 0;
            gap: 2px;
        }

        .bdd-step-icon svg:first-child {
            width: 14px;
            height: 14px;
            flex-shrink: 0;
            stroke-width: 1.3;
        }

        .bdd-step-icon.passed { color: var(--green); }
        .bdd-step-icon.failed { color: var(--red); }
        .bdd-step-icon.skipped { color: var(--amber); }
        .bdd-step-icon.not-run { color: var(--text-muted); }
        .bdd-step-icon.failed svg:first-child,
        .bdd-step-icon.skipped svg:first-child,
        .bdd-step-icon.not-run svg:first-child { background: color-mix(in srgb, currentColor 10%, transparent); border-radius: 50%; }

        .bdd-step-docstring {
            margin: 4px 0 4px 58px;
            background: var(--bg);
            padding: 8px 12px;
            font-family: var(--font-mono);
            font-size: 0.78rem;
            white-space: pre-wrap;
            word-break: break-word;
            overflow-wrap: anywhere;
            border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
            color: var(--text-primary);
        }

        .bdd-table-scroll {
            overflow-x: auto;
            max-width: 100%;
            margin: 4px 0 4px 58px;
        }

        .bdd-step-datatable {
            margin: 0;
            border-collapse: collapse;
            font-size: 0.78rem;
        }

        .bdd-step-datatable th, .bdd-step-datatable td {
            border: 1px solid var(--border);
            padding: 4px 10px;
            text-align: left;
            overflow-wrap: break-word;
            max-width: 320px;
        }

        .bdd-step-datatable thead tr {
            background: var(--bg);
            font-weight: 600;
        }

        .bdd-step-artifacts { margin: 2px 0 2px 58px; }

        .bdd-step-api-toggle { display: none; }

        .bdd-step-artifacts-body {
            max-height: 0;
            overflow: hidden;
            position: relative;
            transition: max-height 0.15s ease-out;
        }

        .bdd-step-artifacts-body.expanded {
            max-height: none;
            overflow-x: auto;
        }

        .bdd-step-toggle-chevron {
            display: inline-flex;
            align-items: center;
            margin-left: auto;
            color: var(--text-muted);
        }

        .bdd-step-toggle-chevron .chevron-svg {
            width: 12px;
            height: 12px;
            transition: transform 0.15s;
        }

        .bdd-step.error-expanded .bdd-step-toggle-chevron .chevron-svg {
            transform: rotate(180deg);
        }

        .bdd-step-error-chevron {
            display: inline-flex;
            align-items: center;
            margin-left: 2px;
        }

        .bdd-step-error-chevron .chevron-svg {
            width: 11px;
            height: 11px;
            transition: transform 0.15s;
        }

        .bdd-step-error-row {
            max-height: 600px;
            overflow: hidden;
            transition: max-height 0.15s ease-out;
        }

        .bdd-step-error-row.collapsed {
            max-height: 0;
        }

        .bdd-step-error {
            margin: 4px 0 4px 58px;
            background: var(--red-bg);
            border: 1px solid var(--red-border);
            border-radius: var(--radius-sm);
            padding: 8px 12px;
            font-size: 0.8rem;
        }

        .bdd-step-error-title {
            font-weight: 600;
            color: var(--red);
            margin-bottom: 4px;
        }

        .bdd-step-stack {
            white-space: pre-wrap;
            overflow-wrap: anywhere;
            color: var(--text-secondary);
            max-height: 200px;
            overflow-y: auto;
            overflow-x: auto;
            font-family: var(--font-mono);
            font-size: 0.78rem;
        }

        .bdd-background-step { opacity: 0.75; }

        footer {
            text-align: center;
            color: var(--text-muted);
            font-size: 0.78rem;
        }

        .chevron-svg {
            width: 14px;
            height: 14px;
            display: inline-block;
            vertical-align: middle;
            flex-shrink: 0;
            transition: transform 0.15s ease;
            color: var(--text-muted);
        }

        /* Rotation states */
        .test-suite-header.collapsed .chevron-svg { transform: rotate(-90deg); }
        .test-case-header.expanded .chevron-svg { transform: rotate(180deg); }
        .api-call-header.expanded .chevron-svg { transform: rotate(180deg); }
        .artifact-header.expanded .chevron-svg { transform: rotate(180deg); }
        .bdd-step.step-expanded .bdd-step-http-chevron .chevron-svg { transform: rotate(180deg); }
        .bdd-step.error-expanded .bdd-step-error-chevron .chevron-svg { transform: rotate(180deg); }

        /* ── Dark Mode ── */
        :root[data-theme='dark'] {
            --bg: #212529;
            --surface: #2b3035;
            --surface-raised: #343a40;
            --surface-overlay: #3d4349;
            --border: var(--color-gray-700);
            --border-light: #3d4349;
            --text-primary: #f8f9fa;
            --text-secondary: #dee2e6;
            --text-muted: #adb5bd;
            --logo-tile-fill: #343a40;
            --logo-tile-stroke: var(--color-gray-700);
            --logo-pulse-stroke: var(--color-teal-400);
            --accent: var(--color-teal-400);
            --accent-light: #2b3035;
            --green: #75b798;
            --red: #ea868f;
            --red-bg: #2c0b0e;
            --red-border: #58151c;
            --amber: #e4cf6e;
            --blue: #6edff6;
            --blue-bg: #032830;
            --blue-border: #055160;
            --violet: #a78bfa;
            --orange: #fb923c;
            --not-run: #dee2e6;
            color-scheme: dark;
        }

        /* No-JS fallback: follow the OS theme only when no preference is stored */
        @media screen and (prefers-color-scheme: dark) {
            :root:not([data-theme]) {
                --bg: #212529;
                --surface: #2b3035;
                --surface-raised: #343a40;
                --surface-overlay: #3d4349;
                --border: var(--color-gray-700);
                --border-light: #3d4349;
                --text-primary: #f8f9fa;
                --text-secondary: #dee2e6;
                --text-muted: #adb5bd;
                --logo-tile-fill: #343a40;
                --logo-tile-stroke: var(--color-gray-700);
                --logo-pulse-stroke: var(--color-teal-400);
                --accent: var(--color-teal-400);
                --accent-light: #2b3035;
                --green: #75b798;
                --red: #ea868f;
                --red-bg: #2c0b0e;
                --red-border: #58151c;
                --amber: #e4cf6e;
                --blue: #6edff6;
                --blue-bg: #032830;
                --blue-border: #055160;
                --violet: #a78bfa;
                --orange: #fb923c;
                --not-run: #dee2e6;
            }
        }

        :root[data-theme='dark'] .class-file-header,
        :root[data-theme='dark'] .metric-chip,
        :root[data-theme='dark'] .artifact-code,
        :root[data-theme='dark'] .bdd-step-docstring,
        :root[data-theme='dark'] .bdd-step-datatable thead tr,
        :root[data-theme='dark'] .api-call-header {
            background: var(--surface-raised);
        }

        :root[data-theme='dark'] .metric-chip,
        :root[data-theme='dark'] .api-call-card,
        :root[data-theme='dark'] .api-call-header,
        :root[data-theme='dark'] .artifact-code,
        :root[data-theme='dark'] .bdd-step-docstring {
            border-color: var(--border-light);
        }

        :root[data-theme='dark'] .test-suite-header:hover,
        :root[data-theme='dark'] .test-case-header:hover,
        :root[data-theme='dark'] .api-call-header:hover,
        :root[data-theme='dark'] .artifact-header:hover,
        :root[data-theme='dark'] .artifact-content a:hover {
            background: var(--surface-overlay);
        }

        :root[data-theme='dark'] .status-badge.not-run {
            color: var(--text-secondary);
            border-color: var(--border-light);
        }

        :root[data-theme='dark'] .artifact-item,
        :root[data-theme='dark'] .test-case,
        :root[data-theme='dark'] .test-case-content,
        :root[data-theme='dark'] .class-file-header {
            border-color: var(--border-light);
        }

        :root[data-theme='dark'] .copy-btn {
            background: rgba(30, 30, 30, 0.92);
        }

        /* bdd-step hover in dark mode */
        :root[data-theme='dark'] .bdd-step:hover { background: var(--surface-raised); }

        .theme-toggle {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 37px;
            height: 37px;
            border-radius: 20px;
            border: 1px solid var(--border);
            background: var(--bg);
            color: var(--text-secondary);
            cursor: pointer;
            flex-shrink: 0;
        }

        .theme-toggle:hover {
            border-color: var(--accent);
            color: var(--accent);
        }

        .theme-icon { width: 16px; height: 16px; }

        @media screen and (max-width: 900px) {
            .report-hero {
                padding: 22px;
            }

            .report-hero-body {
                flex-wrap: wrap;
            }

            .report-hero-side {
                width: 100%;
                flex-basis: 100%;
                align-items: stretch;
            }

            .report-hero-summary {
                width: 100%;
            }
        }

        @media screen and (max-width: 640px) {
            .container {
                padding: 20px 14px 28px;
            }

            .header-top {
                margin-bottom: 18px;
            }

            .header-top .header-actions {
                width: 100%;
                justify-content: space-between;
                margin-left: 0;
            }

            .report-hero {
                padding: 18px;
            }

            .report-hero-title {
                font-size: 1.85rem;
            }

            .report-hero-subtitle {
                font-size: 0.92rem;
            }

            .report-hero-meta {
                gap: 8px;
            }

            .report-hero .report-meta-pill,
            .report-hero .hero-status-pill {
                width: 100%;
                justify-content: flex-start;
            }

            .report-hero-summary-grid {
                grid-template-columns: 1fr;
            }


            .report-navbar .compact-stat-value {
                font-size: 0.88rem;
            }

            .report-navbar-stats {
                display: none;
            }

            .bdd-table-scroll {
                margin-left: 0;
            }
        }

        @media screen and (max-width: 480px) {
            .filter-input {
                flex: 1 1 100%;
            }
        }

        /* ── Filter Bar ── */
        .filter-bar {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 8px;
            margin-bottom: 16px;
            padding: 10px 14px;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            background: var(--surface);
        }

        .filter-input {
            flex: 1 1 200px;
            min-width: 0;
            padding: 7px 12px;
            border: 1px solid var(--border);
            border-radius: 20px;
            background: var(--bg);
            color: var(--text-primary);
            font-size: 0.85rem;
            font-family: var(--font);
            outline: none;
        }

        .filter-input:focus {
            border-color: var(--accent);
        }

        .filter-input::placeholder { color: var(--text-muted); }

        .filter-btns { display: contents; }

        .filter-btn,
        .tag-filter-btn,
        .expand-btn {
            height: 30px;
            padding: 6px 14px;
            border-radius: 20px;
            border: 1px solid var(--border);
            background: var(--bg);
            color: var(--text-secondary);
            font-size: 0.78rem;
            font-weight: 600;
            cursor: pointer;
            white-space: nowrap;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
        }

        .expand-btn {
            padding: 6px 10px;
        }

        .filter-btn:not(.active):hover,
        .tag-filter-btn:not(.has-selection):hover,
        .expand-btn:not(.active):hover {
            color: var(--accent);
            border-color: var(--accent);
        }

        .filter-btn.active,
        .tag-filter-btn.has-selection,
        .expand-btn.active {
            background: var(--accent);
            color: #fff;
            border-color: var(--accent);
        }

        .filter-btn-failed:not(.active):hover {
            background: var(--bg);
            color: var(--red);
            border-color: var(--red);
        }
        .filter-btn-passed:not(.active):hover {
            background: var(--bg);
            color: var(--green);
            border-color: var(--green);
        }
        .filter-btn-skipped:not(.active):hover {
            background: var(--bg);
            color: var(--amber);
            border-color: var(--amber);
        }
        .filter-btn-failed.active {
            background: var(--red);
            border-color: var(--red);
            color: #fff;
        }
        .filter-btn-passed.active {
            background: var(--green);
            border-color: var(--green);
            color: #fff;
        }
        .filter-btn-skipped.active {
            background: var(--amber);
            border-color: var(--amber);
            color: #fff;
        }
        :root[data-theme='dark'] .filter-btn-failed.active,
        :root[data-theme='dark'] .filter-btn-passed.active,
        :root[data-theme='dark'] .filter-btn-skipped.active {
            color: #fff;
        }

        .expand-btns { display: contents; }

        #expandCollapseBtn .ec-arrow-top {
            transition: transform 0.3s ease;
            transform-origin: 12px 6.5px;
        }

        #expandCollapseBtn .ec-arrow-bottom {
            transition: transform 0.3s ease;
            transform-origin: 12px 17.5px;
        }

        #expandCollapseBtn.rotated .ec-arrow-top,
        #expandCollapseBtn.rotated .ec-arrow-bottom {
            transform: scaleY(-1);
        }

        /* ── HTTP Method Badges ── */
        .method-badge {
            display: inline-block;
            font-size: 0.85rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            vertical-align: middle;
        }

        .method-get    { color: var(--green); }
        .method-post   { color: var(--amber); }
        .method-put    { color: var(--blue); }
        .method-delete { color: var(--red); }
        .method-patch  { color: var(--violet); }
        .method-default { color: #6b7280; }
        .method-head, .method-options, .method-trace, .method-connect { color: #6b7280; }

        /* ── HTTP Status Code Badges ── */
        .sc-badge {
            display: inline-block;
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 0.04em;
            vertical-align: middle;
        }
        .sc-1xx { color: var(--blue); }
        .sc-2xx { color: var(--green); }
        .sc-3xx { color: var(--amber); }
        .sc-4xx { color: var(--orange); }
        .sc-5xx { color: var(--red); }

        /* ── Copy Button ── */
        .code-wrapper { position: relative; }

        .copy-btn {
            position: absolute;
            top: 8px;
            right: 8px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 4px 10px;
            border-radius: 20px;
            border: 1px solid var(--border);
            background: rgba(255,255,255,0.85);
            color: var(--text-secondary);
            font-size: 0.72rem;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.15s, color 0.15s, opacity 0.15s;
            opacity: 0;
            pointer-events: none;
        }

        .code-wrapper:hover .copy-btn { opacity: 1; pointer-events: auto; }

        .copy-btn:hover {
            background: var(--accent-light);
            color: var(--accent);
            border-color: var(--accent);
        }

        .copy-btn.copied {
            background: var(--accent-light);
            color: var(--accent);
            border-color: var(--accent);
            opacity: 1;
            pointer-events: none;
        }

        .copy-btn svg { width: 12px; height: 12px; }

        /* ── Footer Enhancement ── */
        .report-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 8px;
            padding: 20px 0 16px;
            color: var(--text-muted);
            font-size: 0.78rem;
        }

        .report-footer strong { color: var(--text-secondary); }
    </style>
    <script>
        (function() {
            var theme = null;
            try {
                var stored = localStorage.getItem('pulse-report-theme');
                if (stored === 'dark' || stored === 'light') {
                    theme = stored;
                } else if (stored === 'true' || stored === '1') {
                    theme = 'dark';
                } else if (stored === 'false' || stored === '0') {
                    theme = 'light';
                }
            } catch (e) {
                theme = null;
            }
            if (theme === null) {
                theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
            }
            document.documentElement.setAttribute('data-theme', theme);
        })();
    </script>
    <script>
        function openLightbox(src) {
            var lb = document.getElementById('screenshot-lightbox');
            document.getElementById('screenshot-lightbox-img').src = src;
            lb.classList.add('open');
        }
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') document.getElementById('screenshot-lightbox').classList.remove('open');
        });

        function smoothExpand(el) {
            el.style.maxHeight = el.scrollHeight + 'px';
            el.classList.add('expanded');
            function onEnd() {
                el.removeEventListener('transitionend', onEnd);
                if (el.classList.contains('expanded')) {
                    el.style.maxHeight = 'none';
                }
            }
            el.addEventListener('transitionend', onEnd);
        }

        function smoothCollapse(el) {
            el.style.maxHeight = el.scrollHeight + 'px';
            el.offsetHeight; // force reflow
            el.style.maxHeight = '0';
            el.classList.remove('expanded');
        }

        function toggleArtifact(header) {
            const content = header.nextElementSibling;
            const isExpanded = content.classList.contains('expanded');
            
            if (isExpanded) {
                smoothCollapse(content);
                header.classList.remove('expanded');
            } else {
                smoothExpand(content);
                header.classList.add('expanded');
            }
        }
        
        function toggleTestCase(header) {
            if (!header.classList.contains('expandable')) {
                return;
            }

            const body = header.nextElementSibling;
            if (!body || !body.classList.contains('test-case-body')) {
                return;
            }

            const isExpanded = body.classList.contains('expanded');
            
            if (isExpanded) {
                body.classList.remove('expanded');
                header.classList.remove('expanded');
            } else {
                body.classList.add('expanded');
                header.classList.add('expanded');
            }
        }
        
        function toggleApiCall(header) {
            const body = header.nextElementSibling;
            const isExpanded = body.classList.contains('expanded');
            
            if (isExpanded) {
                smoothCollapse(body);
                header.classList.remove('expanded');
            } else {
                smoothExpand(body);
                header.classList.add('expanded');
            }
        }

        function toggleSuite(header) {
            const body = header.nextElementSibling;
            const isExpanded = !body.classList.contains('collapsed');
            if (isExpanded) {
                body.classList.add('collapsed');
                header.classList.add('collapsed');
            } else {
                body.classList.remove('collapsed');
                header.classList.remove('collapsed');
            }
        }

        function toggleStep(li) {
            // Skip past doc strings and data tables to find the artifacts/error row
            var next = li.nextElementSibling;
            while (next && !next.classList.contains('bdd-step-artifacts-row') && !next.classList.contains('bdd-step-error-row') && !next.classList.contains('bdd-step')) {
                next = next.nextElementSibling;
            }
            if (!next) return;
            const chevron = li.querySelector('.bdd-step-toggle-chevron .chevron-svg');
            // Artifacts row
            if (next.classList.contains('bdd-step-artifacts-row')) {
                const body = next.querySelector('.bdd-step-artifacts-body');
                if (!body) return;
                const isExpanded = body.classList.contains('expanded');
                if (isExpanded) {
                    smoothCollapse(body);
                    li.classList.remove('step-expanded');
                    if (chevron) chevron.style.transform = '';
                } else {
                    smoothExpand(body);
                    li.classList.add('step-expanded');
                    if (chevron) chevron.style.transform = 'rotate(180deg)';
                }
                return;
            }
            // Error row
            if (next.classList.contains('bdd-step-error-row')) {
                const isCollapsed = next.classList.contains('collapsed');
                if (isCollapsed) {
                    next.classList.remove('collapsed');
                    li.classList.add('error-expanded');
                    if (chevron) chevron.style.transform = 'rotate(180deg)';
                } else {
                    next.classList.add('collapsed');
                    li.classList.remove('error-expanded');
                    if (chevron) chevron.style.transform = '';
                }
            }
        }

        function toggleStepArtifacts(btn) {
            const body = btn.parentElement.nextElementSibling;
            if (!body) return;
            const isExpanded = body.classList.contains('expanded');
            if (isExpanded) {
                smoothCollapse(body);
                btn.classList.remove('expanded');
            } else {
                smoothExpand(body);
                btn.classList.add('expanded');
            }
        }

        // ── Dark Mode ──
        function updateThemeIcon() {
            var buttons = document.querySelectorAll('.theme-toggle');
            if (!buttons.length) return;
            var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
            buttons.forEach(function(btn) {
                if (isDark) {
                    btn.innerHTML = '<svg class="theme-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>';
                    btn.title = 'Switch to light mode';
                } else {
                    btn.innerHTML = '<svg class="theme-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>';
                    btn.title = 'Switch to dark mode';
                }
                btn.setAttribute('aria-pressed', isDark ? 'true' : 'false');
                btn.setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
            });
        }

        function applyTheme(isDark) {
            document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
            updateThemeIcon();
        }

        function toggleTheme() {
            var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
            applyTheme(!isDark);
            try {
                localStorage.setItem('pulse-report-theme', isDark ? 'light' : 'dark');
            } catch (e) {
                // Ignore storage failures and keep the current page theme.
            }
        }

        // ── Search & Filter ──
        var activeFilter = 'all';
        var selectedTags = [];

        function applyFilter() {
            var inp = document.getElementById('filterInput');
            var q = inp ? inp.value.toLowerCase().trim() : '';
            var status = activeFilter;
            document.querySelectorAll('.test-suite').forEach(function(suite) {
                var cases = suite.querySelectorAll('.test-case');
                var visibleCount = 0;
                cases.forEach(function(tc) {
                    var nameEl = tc.querySelector('.test-case-name');
                    var name = nameEl ? nameEl.textContent.toLowerCase() : '';
                    var badge = tc.querySelector('.test-case-header .status-badge');
                    var tcStatus = '';
                    if (badge) {
                        var classes = badge.className.split(/\s+/);
                        for (var i = 0; i < classes.length; i++) {
                            if (classes[i] !== 'status-badge' && classes[i] !== '') { tcStatus = classes[i]; break; }
                        }
                    }
                    var matchName = q === '' || name.indexOf(q) >= 0;
                    var matchStatus = status === 'all' || tcStatus === status;
                    var matchTags = true;
                    if (selectedTags.length > 0) {
                        var tcTags = tc.getAttribute('data-tags');
                        if (tcTags) {
                            var tagList = tcTags.split(',');
                            matchTags = selectedTags.some(function(st) { return tagList.indexOf(st) >= 0; });
                        } else {
                            matchTags = false;
                        }
                    }
                    if (matchName && matchStatus && matchTags) {
                        tc.style.display = '';
                        visibleCount++;
                    } else {
                        tc.style.display = 'none';
                    }
                });
                var suiteBody = suite.querySelector('.test-suite-body');
                var suiteHeader = suite.querySelector('.test-suite-header');
                if (visibleCount > 0) {
                    suite.style.display = '';
                    var filterActive = q !== '' || status !== 'all' || selectedTags.length > 0;
                    if (filterActive || isAllExpanded) {
                        if (suiteBody) suiteBody.classList.remove('collapsed');
                        if (suiteHeader) suiteHeader.classList.remove('collapsed');
                    } else {
                        if (suiteBody) suiteBody.classList.add('collapsed');
                        if (suiteHeader) suiteHeader.classList.add('collapsed');
                    }
                } else {
                    suite.style.display = 'none';
                }
            });
        }

        // ── Tag Filter ──
        function initTagFilter() {
            var allTags = new Set();
            document.querySelectorAll('.test-case[data-tags]').forEach(function(tc) {
                tc.getAttribute('data-tags').split(',').forEach(function(tag) {
                    if (tag.trim()) allTags.add(tag.trim());
                });
            });
            var dropdown = document.getElementById('tagFilterDropdown');
            if (!dropdown || allTags.size === 0) {
                var wrapper = document.querySelector('.tag-filter-wrapper');
                if (wrapper) wrapper.style.display = 'none';
                return;
            }
            var sortedTags = Array.from(allTags).sort();
            var html = '';
            sortedTags.forEach(function(tag) {
                var safeTag = String(tag).replace(/[&<>"']/g, function(c) {
                    return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]);
                });
                html += '<label class="tag-filter-item"><input type="checkbox" value="' + safeTag + '" onchange="onTagCheckChange()"> ' + safeTag + '</label>';
            });
            html += '<button class="tag-filter-clear" onclick="clearTagFilter()">Clear all</button>';
            dropdown.innerHTML = html;
        }

        function toggleTagDropdown() {
            var dropdown = document.getElementById('tagFilterDropdown');
            if (dropdown) dropdown.classList.toggle('open');
        }

        function onTagCheckChange() {
            var dropdown = document.getElementById('tagFilterDropdown');
            var checks = dropdown.querySelectorAll('input[type="checkbox"]');
            selectedTags = [];
            checks.forEach(function(cb) {
                if (cb.checked) selectedTags.push(cb.value);
            });
            var btn = document.getElementById('tagFilterBtn');
            if (selectedTags.length > 0) {
                btn.classList.add('has-selection');
                btn.innerHTML = 'Tags (' + selectedTags.length + ')';
            } else {
                btn.classList.remove('has-selection');
                btn.innerHTML = 'Tags';
            }
            applyFilter();
        }

        function clearTagFilter() {
            var dropdown = document.getElementById('tagFilterDropdown');
            dropdown.querySelectorAll('input[type="checkbox"]').forEach(function(cb) { cb.checked = false; });
            selectedTags = [];
            var btn = document.getElementById('tagFilterBtn');
            btn.classList.remove('has-selection');
            btn.innerHTML = 'Tags';
            applyFilter();
            dropdown.classList.remove('open');
        }

        document.addEventListener('click', function(e) {
            var wrapper = document.querySelector('.tag-filter-wrapper');
            if (wrapper && !wrapper.contains(e.target)) {
                var dropdown = document.getElementById('tagFilterDropdown');
                if (dropdown) dropdown.classList.remove('open');
            }
        });

        function setFilter(btn, status) {
            activeFilter = status;
            document.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
            btn.classList.add('active');
            applyFilter();
        }

        // ── Expand / Collapse All ──
        var isAllExpanded = false;

        function toggleExpandCollapse() {
            var btn = document.getElementById('expandCollapseBtn');
            if (isAllExpanded) {
                document.querySelectorAll('.test-suite-body').forEach(function(b) { b.classList.add('collapsed'); });
                document.querySelectorAll('.test-suite-header').forEach(function(h) { h.classList.add('collapsed'); });
                document.querySelectorAll('.test-case.expandable .test-case-body').forEach(function(b) { b.classList.remove('expanded'); });
                document.querySelectorAll('.test-case-header.expandable').forEach(function(h) { h.classList.remove('expanded'); });
                btn.title = 'Expand all';
                btn.setAttribute('aria-label', 'Expand all');
                btn.classList.remove('rotated');
            } else {
                document.querySelectorAll('.test-suite-body').forEach(function(b) { b.classList.remove('collapsed'); });
                document.querySelectorAll('.test-suite-header').forEach(function(h) { h.classList.remove('collapsed'); });
                document.querySelectorAll('.test-case.expandable .test-case-body').forEach(function(b) { b.classList.add('expanded'); });
                document.querySelectorAll('.test-case-header.expandable').forEach(function(h) { h.classList.add('expanded'); });
                btn.title = 'Collapse all';
                btn.setAttribute('aria-label', 'Collapse all');
                btn.classList.add('rotated');
            }
            isAllExpanded = !isAllExpanded;
        }

        // ── Tag Visibility Toggle ──
        var tagsVisible = true;

        function toggleTagVisibility() {
            tagsVisible = !tagsVisible;
            var btn = document.getElementById('tagVisibilityBtn');
            document.body.classList.toggle('tags-hidden', !tagsVisible);
            if (tagsVisible) {
                btn.classList.remove('active');
                btn.title = 'Hide tags';
            } else {
                btn.classList.add('active');
                btn.title = 'Show tags';
            }
        }

        // ── Copy to Clipboard ──
        var CLIPBOARD_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>';
        var CHECK_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>';

        function addCopyButtons() {
            document.querySelectorAll('pre.artifact-code').forEach(function(pre) {
                var wrapper = document.createElement('div');
                wrapper.className = 'code-wrapper';
                pre.parentNode.insertBefore(wrapper, pre);
                wrapper.appendChild(pre);
                var btn = document.createElement('button');
                btn.className = 'copy-btn';
                btn.innerHTML = CLIPBOARD_SVG + 'Copy';
                btn.onclick = function() {
                    navigator.clipboard.writeText(pre.textContent).then(function() {
                        btn.classList.add('copied');
                        btn.innerHTML = CHECK_SVG + 'Copied!';
                        setTimeout(function() {
                            btn.classList.remove('copied');
                            btn.innerHTML = CLIPBOARD_SVG + 'Copy';
                        }, 1500);
                    }).catch(function() {});
                };
                wrapper.appendChild(btn);
            });
        }

        document.addEventListener('DOMContentLoaded', function() {
            updateThemeIcon();

            var darkMQ = window.matchMedia('(prefers-color-scheme: dark)');

            darkMQ.addEventListener('change', function(e) {
                var storedTheme = null;
                try {
                    storedTheme = localStorage.getItem('pulse-report-theme');
                } catch (err) {
                    storedTheme = null;
                }

                if (storedTheme === 'dark' || storedTheme === 'light') {
                    return;
                }

                applyTheme(e.matches);
            });
            addCopyButtons();
            initTagFilter();
            // Convert UTC timestamps to viewer's local timezone
            document.querySelectorAll('.local-time[data-utc]').forEach(function(el) {
                var raw = el.getAttribute('data-utc');
                try {
                    var d = new Date(raw);
                    if (!isNaN(d.getTime())) {
                        el.textContent = d.toLocaleString(undefined, {
                            year: 'numeric', month: 'short', day: 'numeric',
                            hour: '2-digit', minute: '2-digit', second: '2-digit'
                        });
                    }
                } catch(e) { /* leave raw value on parse error */ }
            });

            // Slim navbar: slide in once the hero has scrolled out of view
            // (hero geometry never changes, so the boundary is stable — no hysteresis needed).
            var heroEl = document.querySelector('.report-hero');
            var navbar = document.getElementById('reportNavbar');
            if (heroEl && navbar && 'IntersectionObserver' in window) {
                new IntersectionObserver(function(entries) {
                    var show = entries[0].boundingClientRect.bottom < 0;
                    navbar.classList.toggle('report-navbar-visible', show);
                    navbar.setAttribute('aria-hidden', show ? 'false' : 'true');
                }, { threshold: [0] }).observe(heroEl);
            }
        });
    </script>
</head>
<body>
<svg aria-hidden="true" style="display:none">
  <defs>
        <symbol id="pulse-mark" viewBox="0 0 48 48" preserveAspectRatio="xMidYMid meet">
            <rect x="2" y="2" width="44" height="44" rx="12" fill="var(--logo-tile-fill)" stroke="var(--logo-tile-stroke)" stroke-width="1.5" vector-effect="non-scaling-stroke"/>
            <path d="M12 24H19L22.5 19L26.5 31L31 16L35 24H38" stroke="var(--logo-pulse-stroke)" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>
        </symbol>
  </defs>
</svg>
<#-- Macro: render an http-request/http-response artifact pair or a single artifact block -->
<#-- Splits HTTP artifacts into separate request/response lists, then zips them into pairs.
     This is resilient to duplicate filter instances (grouped order: [req,req,...,resp,resp,...])
     as well as the normal alternating order ([req,resp,req,resp,...]). -->
<#macro renderArtifacts artifacts>
    <#assign httpReqs = []>
    <#assign httpResps = []>
    <#assign otherArts = []>
    <#list artifacts as a>
        <#if a.type == "http-request">
            <#assign httpReqs = httpReqs + [a]>
        <#elseif a.type == "http-response">
            <#assign httpResps = httpResps + [a]>
        <#else>
            <#assign otherArts = otherArts + [a]>
        </#if>
    </#list>
    <#-- Determine number of API call pairs -->
    <#assign pairCount = httpReqs?size>
    <#if (httpResps?size > pairCount)><#assign pairCount = httpResps?size></#if>
    <#if (pairCount > 0)>
        <#list 0..<pairCount as pi>
            <#assign req = {}>
            <#assign rsp = {}>
            <#if (pi < httpReqs?size)><#assign req = httpReqs[pi]></#if>
            <#if (pi < httpResps?size)><#assign rsp = httpResps[pi]></#if>
            <#-- Extract method & path from request content -->
            <#assign mth = "API"><#assign pth = "Call">
            <#if req.content?? && req.content?has_content && req.content?contains(" ")>
                <#assign pts = req.content?split("\n")?first?split(" ")>
                <#if pts?size gt 0><#assign mth = pts[0]></#if>
                <#if pts?size gt 1>
                    <#assign pth = pts[1]>
                    <#if pth?contains("://")><#assign pp = pth?split("://")><#if pp?size gt 1><#assign pp2 = pp[1]><#if pp2?contains("/")><#assign pth = "/" + (pp2?keep_after("/"))!""></#if></#if></#if>
                </#if>
            </#if>
            <#if (pairCount > 1)>
            <div class="api-call-card">
                <div class="api-call-header" onclick="toggleApiCall(this)">
                    <span class="api-call-title"><span class="method-badge method-${mth?lower_case}">${mth}</span> ${pth}</span>
                    <span class="api-call-toggle"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
                </div>
                <div class="api-call-body">
            </#if>
            <#if req.type?? && req.type == "http-request">
                <div class="artifact-item">
                    <div class="artifact-header" onclick="toggleArtifact(this)">
                        <span class="artifact-type">REQUEST</span>
                        <span class="artifact-duration"><span class="method-badge method-${mth?lower_case}">${mth}</span> ${pth}</span>
                        <span class="toggle-icon"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
                    </div>
                    <#if req.content??><div class="artifact-content"><pre class="artifact-code">${prettyPrintHttpBody(req.content)?html}</pre></div></#if>
                </div>
            </#if>
            <#if rsp.type?? && rsp.type == "http-response">
                <div class="artifact-item">
                    <div class="artifact-header" onclick="toggleArtifact(this)">
                        <span class="artifact-type">RESPONSE</span><#if rsp.content?? && rsp.content?starts_with("Status: ")><#assign _rst = rsp.content?split("\n")?first?split(" ")?last?trim><#if _rst?length gt 0><#assign _rsc = _rst?substring(0,1)><#if _rsc == "1"><#assign _rscls = "sc-1xx"><#elseif _rsc == "2"><#assign _rscls = "sc-2xx"><#elseif _rsc == "3"><#assign _rscls = "sc-3xx"><#elseif _rsc == "4"><#assign _rscls = "sc-4xx"><#else><#assign _rscls = "sc-5xx"></#if><span class="sc-badge ${_rscls}">${_rst}</span></#if></#if><span class="artifact-duration"></span><span class="toggle-icon"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
                    </div>
                    <#if rsp.content??><div class="artifact-content"><pre class="artifact-code">${prettyPrintHttpBody(rsp.content)?html}</pre></div></#if>
                </div>
            </#if>
            <#if (pairCount > 1)>
                </div>
            </div>
            </#if>
        </#list>
    </#if>
    <#list otherArts as oa>
    <div class="artifact-item">
        <div class="artifact-header" onclick="toggleArtifact(this)">
            <span class="artifact-type">${oa.type}</span>
            <span class="artifact-duration">${oa.name}</span>
            <span class="toggle-icon"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
        </div>
        <div class="artifact-content">
            <#if oa.mimeType?? && oa.mimeType?starts_with("image/")>
                <#-- Prefer embedded base64 content (auto-captured screenshots); fall back to reading from path -->
                <#if oa.content?? && oa.content?has_content>
                    <#assign _imgSrc = "data:" + oa.mimeType + ";base64," + oa.content>
                <#else>
                    <#assign _imgSrc = toDataUri(oa.path, oa.mimeType)>
                </#if>
                <#if _imgSrc?has_content>
                <div class="screenshot-wrap"><img class="screenshot-thumb" src="${_imgSrc?html}" alt="${oa.name?html}" onclick="openLightbox(this.src)" /></div>
                <#else><a href="${oa.path?html}" download="${oa.name?html}">Download ${oa.name?html}</a></#if>
            <#elseif oa.mimeType?? && oa.mimeType?starts_with("video/")>
                <#if oa.content?? && oa.content?has_content>
                <div class="video-wrap"><video class="artifact-video" controls preload="metadata"><source src="data:${oa.mimeType?html};base64,${oa.content}" type="${oa.mimeType?html}"></video></div>
                <#else>
                <div class="video-wrap"><video class="artifact-video" controls preload="metadata"><source src="${oa.path?html}" type="${oa.mimeType?html}"></video></div>
                </#if>
            <#elseif oa.content??><#if oa.mimeType?? && oa.mimeType?contains("json")><pre class="artifact-code">${prettyPrintJson(oa.content)?html}</pre><#elseif oa.mimeType?? && oa.mimeType?contains("xml")><pre class="artifact-code">${prettyPrintXml(oa.content)?html}</pre><#else><pre class="artifact-code">${oa.content?html}</pre></#if>
            <#else><a href="${oa.path}" download="${oa.name}">Download ${oa.name}</a></#if>
        </div>
    </div>
    </#list>
</#macro>
    <#function formatDuration ms>
        <#assign secs = (ms / 1000)>
        <#if (secs < 60)>
            <#return secs?string("0.00") + "s">
        <#elseif (secs < 3600)>
            <#assign mins = (secs / 60)?floor>
            <#assign remSecs = secs - mins * 60>
            <#return mins + "m " + remSecs?string("0") + "s">
        <#else>
            <#assign hrs = (secs / 3600)?floor>
            <#assign remMins = ((secs - hrs * 3600) / 60)?floor>
            <#assign remSecs = secs - hrs * 3600 - remMins * 60>
            <#return hrs + "h " + remMins + "m " + remSecs?string("0") + "s">
        </#if>
    </#function>
    <#assign environment = testRun.environment!{}>
    <#assign browserName = environment["browser"]!"">
    <#assign browserVersion = environment["browserVersion"]!"">
    <#assign platformName = environment["platform"]!"">
    <#-- Mobile session metadata (recorded via AppiumAdapter.recordSessionMetadata) -->
    <#assign mobPlatform = environment["mobile.platform"]!"">
    <#assign mobPlatformVersion = environment["mobile.platformVersion"]!"">
    <#assign mobDeviceName = environment["mobile.deviceName"]!"">
    <#assign mobDeviceModel = environment["mobile.deviceModel"]!"">
    <#assign mobAppVersion = environment["mobile.appVersion"]!"">
    <#assign mobAppiumVersion = environment["mobile.appiumServerVersion"]!"">
    <#assign mobUdid = environment["mobile.udid"]!"">
    <#assign mobAutomation = environment["mobile.automationName"]!"">
    <#assign heroStatusValue = (testRun.status!"")?string>
    <#assign heroStatusClass = heroStatusValue?lower_case?replace("_", "-")?replace(" ", "-")>
    <#assign hasBrowserMetadata = browserName?has_content || browserVersion?has_content || platformName?has_content>
    <#assign isWebRun = false>
    <#list testRun.suites![] as suite>
        <#if !isWebRun>
            <#list suite.testCases![] as testCase>
                <#if !isWebRun>
                    <#list testCase.metrics![] as metric>
                        <#assign metricName = (metric.name!"")?lower_case>
                        <#if metricName == "page.load.time"
                            || metricName == "dom.ready.time"
                            || metricName?starts_with("network.")>
                            <#assign isWebRun = true>
                        </#if>
                    </#list>
                </#if>
                <#if !isWebRun>
                    <#list testCase.artifacts![] as artifact>
                        <#assign artifactType = (artifact.type!"")?lower_case>
                        <#if ((artifactType == "screenshot") && hasBrowserMetadata)
                            || artifactType == "browser-log"
                            || artifactType == "console-log"
                            || artifactType == "har">
                            <#assign isWebRun = true>
                        </#if>
                    </#list>
                </#if>
            </#list>
        </#if>
    </#list>
    <#assign suiteCount = (testRun.suites![])?size>
    <nav class="report-navbar" id="reportNavbar" aria-hidden="true">
        <div class="report-navbar-inner">
            <div class="brand">
                <svg class="brand-icon" viewBox="0 0 48 48" aria-hidden="true">
                    <use href="#pulse-mark" x="0" y="0" width="48" height="48"/>
                </svg>
                <div class="brand-wordmark">
                    <div class="brand-title">PulseReport</div>
                </div>
            </div>
            <div class="report-navbar-stats">
                <div class="compact-stat"><span class="compact-stat-label">Tests</span><span class="compact-stat-value">${testRun.totalTests}</span></div>
                <div class="compact-stat"><span class="compact-stat-label">Suites</span><span class="compact-stat-value">${suiteCount}</span></div>
                <div class="compact-stat"><span class="compact-stat-label">Pass</span><span class="compact-stat-value"><#if (testRun.totalTests > 0)>${(testRun.passedTests / testRun.totalTests * 100)?string("0.#")}%<#else>0%</#if></span></div>
                <div class="compact-stat"><span class="compact-stat-label">Duration</span><span class="compact-stat-value">${formatDuration(testRun.duration)}</span></div>
            </div>
            <div class="header-actions">
                <span class="report-meta-pill status-pill hero-status-pill ${heroStatusClass}">Status: ${testRun.status}</span>
                <button class="theme-toggle" onclick="toggleTheme()" aria-label="Toggle dark mode" title="Toggle dark mode"></button>
            </div>
        </div>
    </nav>
    <div class="container">
        <header class="report-hero">
            <div class="header-top">
                <div>
                    <div class="brand">
                        <svg class="brand-icon" viewBox="0 0 48 48" aria-hidden="true">
                            <use href="#pulse-mark" x="0" y="0" width="48" height="48"/>
                        </svg>
                        <div class="brand-wordmark">
                            <div class="brand-title">PulseReport</div>
                            <div class="brand-subtitle">Automated test results</div>
                        </div>
                    </div>
                </div>
                <div class="header-actions">
                    <span class="report-meta-pill status-pill hero-status-pill ${heroStatusClass}">Status: ${testRun.status}</span>
                    <button class="theme-toggle" id="themeToggle" onclick="toggleTheme()" aria-label="Toggle dark mode" title="Toggle dark mode"></button>
                </div>
            </div>
            <div class="report-hero-body">
                <div class="report-hero-content">
                    <div class="report-hero-kicker">Test run</div>
                    <h1 class="report-hero-title">${testRun.name?html}</h1>
                    <p class="report-hero-subtitle">PulseReport execution overview with ${suiteCount} suite<#if suiteCount != 1>s</#if> and ${testRun.totalTests} test<#if testRun.totalTests != 1>s</#if>.</p>
                    <div class="report-hero-meta">
                        <span class="report-meta-pill report-meta-pill-icon report-meta-pill-started">
                            <span class="report-meta-icon" aria-hidden="true"><svg viewBox="0 0 16 16" fill="none" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="5.25"></circle><path d="M6.95 5.85 9.95 8l-3 2.15Z"></path></svg></span>
                            <span class="sr-only">Started </span>
                            <span class="local-time report-meta-value" data-utc="${testRun.startTime}">${testRun.startTime}</span>
                        </span>
                        <#if testRun.endTime??>
                        <span class="report-meta-pill report-meta-pill-icon report-meta-pill-ended">
                            <span class="report-meta-icon" aria-hidden="true"><svg viewBox="0 0 16 16" fill="none" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4.75 13V3.25"></path><path d="M5 3.5h5.2c.9 0 1.55.73 1.55 1.55 0 .44-.18.86-.5 1.16L10.2 7.3l1.05 1.08c.32.3.5.72.5 1.16 0 .82-.65 1.55-1.55 1.55H5"></path></svg></span>
                            <span class="sr-only">Ended </span>
                            <span class="local-time report-meta-value" data-utc="${testRun.endTime}">${testRun.endTime}</span>
                        </span>
                        </#if>
                        <#if isWebRun && browserName?has_content>
                        <span class="report-meta-pill">Browser: ${browserName}<#if browserVersion?has_content> ${browserVersion}</#if></span>
                        </#if>
                        <#if isWebRun && platformName?has_content>
                        <span class="report-meta-pill">Platform: ${platformName}</span>
                        </#if>
                        <#if mobPlatform?has_content>
                        <span class="report-meta-pill">Platform: ${mobPlatform?html}<#if mobPlatformVersion?has_content> ${mobPlatformVersion?html}</#if></span>
                        </#if>
                        <#if mobDeviceName?has_content || mobDeviceModel?has_content>
                        <span class="report-meta-pill">Device: <#if mobDeviceName?has_content>${mobDeviceName?html}<#else>${mobDeviceModel?html}</#if></span>
                        </#if>
                        <#if mobUdid?has_content>
                        <span class="report-meta-pill">UDID: ${mobUdid?html}</span>
                        </#if>
                        <#if mobAutomation?has_content>
                        <span class="report-meta-pill">Automation: ${mobAutomation?html}</span>
                        </#if>
                        <#if mobAppVersion?has_content>
                        <span class="report-meta-pill">App: ${mobAppVersion?html}</span>
                        </#if>
                        <#if mobAppiumVersion?has_content>
                        <span class="report-meta-pill">Appium: ${mobAppiumVersion?html}</span>
                        </#if>
                    </div>
                </div>
                <div class="report-hero-side">
                    <div class="report-hero-summary">
                        <div class="report-hero-summary-label">Run snapshot</div>
                        <div class="report-hero-summary-grid">
                            <div class="report-hero-stat">
                                <span class="report-hero-stat-label">Tests</span>
                                <span class="report-hero-stat-value">${testRun.totalTests}</span>
                            </div>
                            <div class="report-hero-stat">
                                <span class="report-hero-stat-label">Suites</span>
                                <span class="report-hero-stat-value">${suiteCount}</span>
                            </div>
                            <div class="report-hero-stat">
                                <span class="report-hero-stat-label">Pass rate</span>
                                <span class="report-hero-stat-value"><#if (testRun.totalTests > 0)>${(testRun.passedTests / testRun.totalTests * 100)?string("0.##")}%<#else>0%</#if></span>
                            </div>
                            <div class="report-hero-stat">
                                <span class="report-hero-stat-label">Duration</span>
                                <span class="report-hero-stat-value">${formatDuration(testRun.duration)}</span>
                            </div>
                        </div>
                        <div class="report-hero-breakdown">
                            <span class="report-hero-breakdown-item pass">${testRun.passedTests} passed</span>
                            <span class="report-hero-breakdown-item fail">${testRun.failedTests} failed</span>
                            <span class="report-hero-breakdown-item skip">${testRun.skippedTests} skipped</span>
                        </div>
                    </div>
                </div>
            </div>
        </header>

        <div class="filter-bar" role="search">
            <input type="text" class="filter-input" id="filterInput" placeholder="Filter tests…" oninput="applyFilter()" aria-label="Filter tests by name">
            <div class="filter-btns" role="group" aria-label="Filter by status">
                <button class="filter-btn active" onclick="setFilter(this, 'all')">All</button>
                <button class="filter-btn filter-btn-failed" onclick="setFilter(this, 'failed')">Failed</button>
                <button class="filter-btn filter-btn-passed" onclick="setFilter(this, 'passed')">Passed</button>
                <button class="filter-btn filter-btn-skipped" onclick="setFilter(this, 'skipped')">Skipped</button>
            </div>
            <div class="tag-filter-wrapper">
                <button class="tag-filter-btn" onclick="toggleTagDropdown()" id="tagFilterBtn">
                    Tags
                </button>
                <div class="tag-filter-dropdown" id="tagFilterDropdown"></div>
            </div>
            <button class="expand-btn" id="tagVisibilityBtn" onclick="toggleTagVisibility()" title="Show/hide tags" aria-label="Toggle tag visibility"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg></button>
            <div class="expand-btns">
                <button class="expand-btn" id="expandCollapseBtn" onclick="toggleExpandCollapse()" title="Expand all" aria-label="Expand all"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline class="ec-arrow-top" points="7 9 12 4 17 9"/><polyline class="ec-arrow-bottom" points="7 15 12 20 17 15"/></svg></button>
            </div>
        </div>

        <#list testRun.suites as suite>
        <#-- If the suite name is a generic Surefire/default name, derive it from the first test class -->
        <#assign suiteDisplayName = suite.name>
        <#assign suiteSecondaryText = "">
        <#assign suiteClassPath = "">
        <#if suite.secondaryText?? && suite.secondaryText?has_content>
            <#assign suiteSecondaryText = suite.secondaryText>
        </#if>
        <#if (suite.name?lower_case?contains("surefire") || suite.name?lower_case == "default suite") && suite.testCases?has_content>
            <#list suite.testCases as _ftc>
                <#if suiteClassPath == "" && _ftc.className?? && !_ftc.bddType??>
                    <#assign _parts = _ftc.className?split(".")>
                    <#assign suiteDisplayName = _parts?last>
                    <#assign suiteClassPath = _ftc.className>
                </#if>
            </#list>
            <#if suiteSecondaryText == "" && suiteClassPath != "">
                <#assign suiteSecondaryText = suiteClassPath>
            </#if>
        </#if>
        <div class="test-suite">
            <div class="test-suite-header ${suite.status?lower_case} collapsed" onclick="toggleSuite(this)">
                <div class="test-suite-header-content">
                    <h2>${suiteDisplayName?html}<#if suiteSecondaryText != ""> <span class="suite-class-path">${suiteSecondaryText?html}</span></#if></h2>
                    <div class="test-suite-stats">
                        <#if suite.tags?? && (suite.tags?size > 0)><span class="test-case-tags" title="${suite.tags?join(', ')?html}"><#list suite.tags as tag><span class="tag-label">${tag}</span><#sep> </#sep></#list></span><span class="suite-stats-sep">&middot;</span></#if>
                        <span class="suite-stats-ratio">${suite.passedTests}/${suite.testCases?size} passed</span>
                        <span class="suite-stats-sep">&middot;</span>
                        <span class="suite-stats-duration">${formatDuration(suite.duration)}</span>
                    </div>
                </div>
                <span class="test-suite-toggle"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
            </div>
            <div class="test-suite-body collapsed">
            <#list suite.testCases as testCase>
            <#assign hasTestError = testCase.errorMessage?? && testCase.errorMessage?has_content>
            <#assign hasArtifacts = testCase.artifacts?? && (testCase.artifacts?size > 0)>
            <#assign hasMetrics = testCase.metrics?? && (testCase.metrics?size > 0)>
            <#assign hasBddSteps = testCase.steps?? && (testCase.steps?size > 0)>
            <#assign hasBackgroundSteps = testCase.backgroundSteps?? && (testCase.backgroundSteps?size > 0)>
            <#assign isExpandable = hasTestError || hasArtifacts || hasMetrics || hasBddSteps || hasBackgroundSteps>
            <div class="test-case<#if isExpandable> expandable</#if>"<#if testCase.tags?? && (testCase.tags?size > 0)> data-tags="${testCase.tags?join(",")}"</#if>>
                <div class="test-case-header<#if isExpandable> expandable</#if>"<#if isExpandable> onclick="toggleTestCase(this)"</#if>>
                    <span class="status-badge ${testCase.status?lower_case}"></span>
                    <span class="test-case-name">
                        <#if testCase.methodName??>${testCase.methodName?html}<#if testCase.name?? && testCase.name != testCase.methodName>
                        <span class="test-case-method">${testCase.name?html}</span></#if><#elseif testCase.name??>${testCase.name?html}</#if><#if testCase.className?? && !testCase.bddType??><span class="suite-class-path">${testCase.className?replace(".", "/")}.java</span></#if>
                        <#if testCase.tags?? && (testCase.tags?size > 0)><span class="test-case-tags" title="${testCase.tags?join(', ')?html}"><#list testCase.tags as tag><span class="tag-label">${tag}</span><#sep> </#sep></#list></span></#if>
                    </span>
                    <span class="test-case-time">${formatDuration(testCase.duration)}</span>
                    <#if isExpandable>
                    <span class="test-case-toggle"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span>
                    </#if>
                </div>
                <#if isExpandable>
                <div class="test-case-body">
                    <div class="test-case-content">
                        <#if testCase.bddType??>
                        <#-- ====== BDD scenario layout ====== -->
                        <#-- Background steps inlined per scenario -->
                        <#if testCase.backgroundSteps?? && (testCase.backgroundSteps?size > 0)>
                        <div class="bdd-background-section">
                            <div class="bdd-background-title">Background</div>
                            <ul class="bdd-steps-list">
                            <#list testCase.backgroundSteps as bStep>
                                <li class="bdd-step bdd-background-step<#if bStep.artifacts?? && (bStep.artifacts?size > 0)> has-artifacts</#if><#if bStep.errorMessage??> has-error error-expanded</#if>"<#if bStep.artifacts?? && (bStep.artifacts?size > 0) || bStep.errorMessage??> onclick="toggleStep(this)"</#if>>
                                    <span class="bdd-step-status"><#if bStep.status == "SKIPPED" && testCase.status == "FAILED"><span class="bdd-step-icon not-run"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/></svg><span class="sr-only">Not run</span></span><#else><span class="bdd-step-icon ${bStep.status?lower_case}"><#if bStep.status == "PASSED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.2 7.2 6.2 9.1 9.9 5.3"/></svg><#elseif bStep.status == "FAILED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M5 5 9 9M9 5 5 9"/></svg><#elseif bStep.status == "SKIPPED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.5 7h5"/></svg><#else><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/></svg></#if><span class="sr-only">${bStep.status}</span></span></#if></span>
                                    <span class="bdd-step-keyword">${bStep.keyword!""}</span>
                                    <span class="bdd-step-name">${bStep.name!""}</span>
                                    <#if bStep.errorMessage?? || (bStep.artifacts?? && (bStep.artifacts?size > 0))><span class="bdd-step-toggle-chevron"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span></#if>
                                </li>
                                <#if bStep.docString??>
                                <li><pre class="bdd-step-docstring">${bStep.docString?html}</pre></li>
                                </#if>
                                <#if bStep.dataTable?? && (bStep.dataTable?size > 0)>
                                <li>
                                    <div class="bdd-table-scroll">
                                    <table class="bdd-step-datatable">
                                        <#list bStep.dataTable as row>
                                        <#if row?is_first><thead><tr><#list row as cell><th>${cell?html}</th></#list></tr></thead><tbody>
                                        <#else><tr><#list row as cell><td>${cell?html}</td></#list></tr>
                                        </#if>
                                        </#list>
                                        </tbody>
                                    </table>
                                    </div>
                                </li>
                                </#if>
                                <#if bStep.artifacts?? && (bStep.artifacts?size > 0)>
                                <li class="bdd-step-artifacts-row"><div class="bdd-step-artifacts-body"><div class="bdd-step-artifacts"><@renderArtifacts artifacts=bStep.artifacts /></div></div></li>
                                </#if>
                                <#if bStep.errorMessage??>
                                <li class="bdd-step-error-row">
                                    <div class="bdd-step-error">
                                        <div class="bdd-step-error-title">${bStep.errorMessage?html}</div>
                                        <#if bStep.stackTrace??><div class="bdd-step-stack">${bStep.stackTrace?html}</div></#if>
                                    </div>
                                </li>
                                </#if>
                            </#list>
                            </ul>
                        </div>
                        </#if>

                        <#-- Scenario / Outline steps -->
                        <#if testCase.steps?? && (testCase.steps?size > 0)>
                        <div class="bdd-scenario-steps-title">Steps</div>
                        <ul class="bdd-steps-list">
                        <#list testCase.steps as sStep>
                            <li class="bdd-step<#if sStep.artifacts?? && (sStep.artifacts?size > 0)> has-artifacts</#if><#if sStep.errorMessage??> has-error error-expanded</#if>"<#if sStep.artifacts?? && (sStep.artifacts?size > 0) || sStep.errorMessage??> onclick="toggleStep(this)"</#if>>
                                <span class="bdd-step-status"><#if sStep.status == "SKIPPED" && testCase.status == "FAILED"><span class="bdd-step-icon not-run"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/></svg><span class="sr-only">Not run</span></span><#else><span class="bdd-step-icon ${sStep.status?lower_case}"><#if sStep.status == "PASSED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.2 7.2 6.2 9.1 9.9 5.3"/></svg><#elseif sStep.status == "FAILED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M5 5 9 9M9 5 5 9"/></svg><#elseif sStep.status == "SKIPPED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.5 7h5"/></svg><#else><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/></svg></#if><span class="sr-only">${sStep.status}</span></span></#if></span>
                                <span class="bdd-step-keyword">${sStep.keyword!""}</span>
                                <span class="bdd-step-name">${sStep.name!""}</span>
                                <#if sStep.errorMessage?? || (sStep.artifacts?? && (sStep.artifacts?size > 0))><span class="bdd-step-toggle-chevron"><svg class="chevron-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg></span></#if>
                            </li>
                            <#if sStep.docString??>
                            <li><pre class="bdd-step-docstring">${sStep.docString?html}</pre></li>
                            </#if>
                            <#if sStep.dataTable?? && (sStep.dataTable?size > 0)>
                            <li>
                                <div class="bdd-table-scroll">
                                <table class="bdd-step-datatable">
                                    <#list sStep.dataTable as row>
                                    <#if row?is_first><thead><tr><#list row as cell><th>${cell?html}</th></#list></tr></thead><tbody>
                                    <#else><tr><#list row as cell><td>${cell?html}</td></#list></tr>
                                    </#if>
                                    </#list>
                                    </tbody>
                                </table>
                                </div>
                            </li>
                            </#if>
                            <#if sStep.artifacts?? && (sStep.artifacts?size > 0)>
                            <li class="bdd-step-artifacts-row"><div class="bdd-step-artifacts-body"><div class="bdd-step-artifacts"><@renderArtifacts artifacts=sStep.artifacts /></div></div></li>
                            </#if>
                            <#if sStep.errorMessage??>
                            <li class="bdd-step-error-row">
                                <div class="bdd-step-error">
                                    <div class="bdd-step-error-title">${sStep.errorMessage?html}</div>
                                    <#if sStep.stackTrace??><div class="bdd-step-stack">${sStep.stackTrace?html}</div></#if>
                                </div>
                            </li>
                            </#if>
                        </#list>
                        </ul>
                        </#if>

                        <#-- Scenario-level error fallback: only when no individual step already shows the error -->
                        <#assign anyStepHasError = false>
                        <#if testCase.steps??><#list testCase.steps as _chk><#if _chk.errorMessage??><#assign anyStepHasError = true></#if></#list></#if>
                        <#if testCase.backgroundSteps??><#list testCase.backgroundSteps as _chk><#if _chk.errorMessage??><#assign anyStepHasError = true></#if></#list></#if>
                        <#if testCase.status == "FAILED" && testCase.errorMessage?? && !anyStepHasError>
                        <div class="error-message">
                            <div class="error-message-title">Error: ${testCase.errorMessage?html}</div>
                            <#if testCase.stackTrace??><div class="stack-trace">${testCase.stackTrace?html}</div></#if>
                        </div>
                        </#if>

                        <#else>
                        <#-- ====== Legacy / non-BDD layout ====== -->
                        <#if testCase.status == "FAILED" && testCase.errorMessage??>
                        <div class="error-message">
                            <div class="error-message-title">Error: ${testCase.errorMessage?html}</div>
                            <#if testCase.stackTrace??>
                            <div class="stack-trace">${testCase.stackTrace?html}</div>
                            </#if>
                        </div>
                        </#if>

                        <#-- Steps recorded via the step API (TestNG/Selenium/Appium) -->
                        <#if testCase.steps?? && (testCase.steps?size > 0)>
                        <div class="bdd-scenario-steps-title">Steps</div>
                        <ul class="bdd-steps-list">
                        <#list testCase.steps as mStep>
                            <li class="bdd-step<#if mStep.errorMessage??> has-error error-expanded</#if>">
                                <span class="bdd-step-status"><span class="bdd-step-icon ${mStep.status?lower_case}"><#if mStep.status == "PASSED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.2 7.2 6.2 9.1 9.9 5.3"/></svg><#elseif mStep.status == "FAILED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M5 5 9 9M9 5 5 9"/></svg><#elseif mStep.status == "SKIPPED"><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/><path d="M4.5 7h5"/></svg><#else><svg viewBox="0 0 14 14" width="14" height="14" aria-hidden="true" focusable="false" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="7" cy="7" r="5.5"/></svg></#if><span class="sr-only">${mStep.status}</span></span></span>
                                <span class="bdd-step-name">${(mStep.name!"")?html}</span>
                                <#if mStep.duration gt 0><span class="bdd-step-duration">${formatDuration(mStep.duration)}</span></#if>
                            </li>
                            <#if mStep.description?? && mStep.description?has_content>
                            <li class="bdd-step-desc-row"><div class="step-description">${mStep.description?html}</div></li>
                            </#if>
                            <#if mStep.errorMessage??>
                            <li class="bdd-step-error-row">
                                <div class="bdd-step-error">
                                    <div class="bdd-step-error-title">${mStep.errorMessage?html}</div>
                                    <#if mStep.stackTrace??><div class="bdd-step-stack">${mStep.stackTrace?html}</div></#if>
                                </div>
                            </li>
                            </#if>
                        </#list>
                        </ul>
                        </#if>

                        <#if testCase.artifacts?? && (testCase.artifacts?size > 0)>
                        <div class="artifact-section">
                            <@renderArtifacts artifacts=testCase.artifacts />
                        </div>
                        </#if>

                        <#if testCase.metrics?? && (testCase.metrics?size > 0)>
                        <div class="metrics-section">
                            <div class="metrics-section-title">Metrics</div>
                            <div class="metrics-grid">
                            <#list testCase.metrics as metric>
                                <div class="metric-chip">
                                    <span class="metric-chip-name">${metric.name}</span>
                                    <span class="metric-chip-value">${metric.value?string("0.##")}</span>
                                    <#if metric.unit??><span class="metric-chip-unit">${metric.unit}</span></#if>
                                </div>
                            </#list>
                            </div>
                        </div>
                        </#if>
                        </#if><#-- end BDD/legacy if -->
                    </div>
                </div>
                </#if>
            </div>
            </#list>
            </div>
        </div>
        </#list>

        <footer class="report-footer">
            <div style="display:flex;align-items:center;gap:7px;">
                <strong>PulseReport</strong> - Automated test results
            </div>
            <div>${testRun.totalTests} tests &middot; ${formatDuration(testRun.duration)} total duration</div>
        </footer>
    </div>
<div id="screenshot-lightbox" onclick="this.classList.remove('open')">
    <img id="screenshot-lightbox-img" src="" alt="Screenshot" />
</div>
</body>
</html>
