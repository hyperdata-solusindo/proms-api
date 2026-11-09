package com.hyperdata.proms.common.docs;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class ScalarDocController {

    private static final String SCALAR_CUSTOM_CSS = """
        /* 1. DOMAIN GROUP (HEADING KATEGORI DOMAIN) */
        aside li[data-sidebar-id*="/tag-group/"] > div:hover,
        aside a[href*="#tag-group/"]:hover {
          background: rgba(168, 85, 247, 0.2) !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > div > a,
        aside li[data-sidebar-id*="/tag-group/"] > div > button,
        aside li[data-sidebar-id*="/tag-group/"] > a,
        aside a[href*="#tag-group/"] {
          padding: 0.35rem 0.5rem !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] [class*="button-label"],
        aside a[href*="#tag-group/"] [class*="button-label"],
        aside a[href*="#tag-group/"] div {
          font-size: 0.72rem !important;
          font-weight: 800 !important;
          text-transform: uppercase !important;
          letter-spacing: 0.08em !important;
          color: #c084fc !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] button,
        aside div:has(> a[href*="#tag-group/"]) button {
          color: #c084fc !important;
        }

        /* 2. FEATURE TAGS (SEJAJAR DENGAN DOMAIN GROUP) */
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > button,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > button,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > div > a,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > div > button,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > a,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > button {
          padding-left: 0.5rem !important;
          padding-top: 0.35rem !important;
          padding-bottom: 0.35rem !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > div > a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > a .scalar-sidebar-indent {
          display: none !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a [class*="button-label"],
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a [class*="button-label"],
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a > div,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a > div,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) [class*="button-label"] {
          font-size: 0.825rem !important;
          font-weight: 600 !important;
          text-transform: none !important;
          letter-spacing: normal !important;
          color: #f1f5f9 !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a:hover,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a:hover,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > div > a:hover,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > a:hover {
          background: rgba(255, 255, 255, 0.06) !important;
          color: #ffffff !important;
        }

        /* 3. ENDPOINTS (INDENTASI MENJOROK KE DALAM DI BAWAH FEATURE TAG) */
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li > a,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > ul > li > a {
          padding-left: 1.65rem !important;
          padding-top: 0.3rem !important;
          padding-bottom: 0.3rem !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li > a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > ul > li > a .scalar-sidebar-indent {
          display: none !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li > a [class*="button-label"],
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > ul > li > a [class*="button-label"] {
          font-size: 0.8rem !important;
          font-weight: 400 !important;
          text-transform: none !important;
          color: #cbd5e1 !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li > a:hover,
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > ul > li > a:hover {
          background: rgba(255, 255, 255, 0.06) !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li > a:hover [class*="button-label"],
        aside li[data-sidebar-id*="/tag/"]:not(li[data-sidebar-id*="/tag-group/"] *) > ul > li > a:hover [class*="button-label"] {
          color: #ffffff !important;
        }

        /* 4. SEMBUNYIKAN SEMUA GARIS PANDUAN INDENTASI (DEFAULT, HOVER, & ACTIVE) */
        :root,
        html,
        body {
          --scalar-sidebar-indent-border: transparent !important;
          --scalar-sidebar-indent-border-hover: transparent !important;
          --scalar-sidebar-indent-border-active: transparent !important;
        }
        .scalar-sidebar-indent-border,
        .scalar-sidebar-indent [class*="indent-border"],
        .scalar-sidebar-indent [class*="w-border"] {
          display: none !important;
          background: transparent !important;
        }
        """;

    @GetMapping(value = "/docs", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> scalarDocs() {
        long timestamp = System.currentTimeMillis();
        String html = """
            <!doctype html>
            <html>
              <head>
                <title>Proms ERP - API Reference</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="icon" type="image/svg+xml" href="https://scalar.com/favicon.svg" />
                <style>
            """ + SCALAR_CUSTOM_CSS
            + """
                  </style>
                </head>
                <body>
                  <script
                    id="api-reference"
                    data-url="/v3/api-docs?v=""" + timestamp
            + """
                      "
                      data-configuration='{
                        "theme": "purple",
                        "darkMode": true,
                        "layout": "modern",
                        "showSidebar": true
                      }'>
                    </script>
                    <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                    <script>
                      (function() {
                        var script = document.createElement('script');
                        script.src = 'http://' + (location.host || 'localhost').split(':')[0] + ':35729/livereload.js?snipver=1';
                        script.onerror = function() {};
                        document.head.appendChild(script);
                      })();
                    </script>
                  </body>
                </html>
                """;
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.EXPIRES, "0")
            .body(html);
    }
}
