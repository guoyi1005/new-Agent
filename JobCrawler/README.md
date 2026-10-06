# JobCrawler

Small, manual, low-rate collection of public internship listings. The first release implements one source: the publicly accessible HTML list/detail pages on Shixiseng (`s.shixiseng.com` / `www.shixiseng.com`). HTTP is used first; this source is server-rendered, so Playwright is not launched. There is no BOSS source, login, cookie, proxy, hidden API, CAPTCHA handling, or homepage-triggered crawl.

## Status: the backend now crawls daily by itself

The same source is implemented in the backend as `ShixisengJobRadarCrawler` plus
`MarketJobRadarScheduler` (`market-job.radar.*` in `AppBackend/src/main/resources/application.yml`),
so 「为你推荐的岗位」 and 「热门岗位」 no longer depend on someone running this script by hand.
The scheduled crawler keeps these rules: public list/detail pages only, 1.5-3s between
requests, a bounded number of requests per run, the same quality score gate, and it stops
the whole run on 403/429 or a verification page without trying to bypass anything.

This Python tool stays useful for two things: a manual re-crawl when the backend source is
blocked, and a review-then-import path where a human reads `output/jobs.json` before pushing it.
Admins can also trigger the backend crawler without any Python environment:

```text
GET  /api/admin/jobs/radar/status
POST /api/admin/jobs/radar/refresh            # 可用 ?keywords=Python&limit=3 做小范围验证
```

## Setup and run

```powershell
cd JobCrawler
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env
python crawl_jobs.py --keywords Python --limit 20
```

The page cap is 30 per keyword. Requests to the same host wait 1–3 seconds. Connect/read timeout is bounded; transient 5xx and transport failures get at most two retries. 403, 429, verification, login, or parse failures stop/mark that source and are not bypassed. The crawl only writes `output/jobs.json` and `output/crawl.log`; it never writes directly to MySQL. The importer excludes explicit offline roles and listings whose source refresh date is older than 90 days, while keeping those records in the review JSON.

Run other normal search terms explicitly when needed:

```powershell
python crawl_jobs.py --keywords Java,Python,AI应用,大模型,前端,数据分析,产品经理 --limit 20
```

Review the JSON and source URLs before import. Then set the admin JWT in `.env` as `APP_API_TOKEN` and run:

```powershell
python import_jobs.py
```

The importer calls the protected `POST /api/admin/jobs/import-batch` endpoint. No data is imported by crawling itself.

## Source assessment (2026-10-02)

- Shixiseng public HTML list and detail pages returned HTTP 200 and directly contained listing anchors and detail fields (title, daily salary, location, education, internship period, description). `s.shixiseng.com/robots.txt` returned 404; the parent `www.shixiseng.com/robots.txt` returned an empty body. Since no explicit robots policy is published on the tested host, this tool stays on the visible search/list and detail pages, uses a low request rate and bounded sample, and should be stopped if the site's current terms or response indicate otherwise.
- Nowcoder `robots.txt` was readable and allowed the tested `/jobs/detail/...` route, but the tested school schedule page was an employer/schedule listing, not a job-card list; not implemented as a source.
- Lagou returned an Alibaba WAF verification page; disabled, with no attempt to solve it.
- Liepin `robots.txt` disallows query-string routes used by search listings; no list source implemented.

Only Shixiseng is implemented. `playwright` remains an optional dependency but is not enabled for this server-rendered source. Offline parser and processor tests use local fixtures and never visit a website.
