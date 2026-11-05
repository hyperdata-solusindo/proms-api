package com.hyperdata.proms.common.docs;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Hidden
@Controller
public class ScalarDocController {

    private static final String SCALAR_CUSTOM_CSS = """
        /* 1. DOMAIN GROUP (PARENT / CATEGORY HEADING) */
        aside li[data-sidebar-id*="/tag-group/"] > div:hover,
        aside a[href*="#tag-group/"]:hover {
          background: rgba(168, 85, 247, 0.2) !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > div a,
        aside li[data-sidebar-id*="/tag-group/"] > div button,
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

        /* 2. FEATURE TAGS (TIDAK ADA PADDING LEFT / SEJAJAR DENGAN DOMAIN GROUP) */
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > button,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > button,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > a,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > div > a,
        aside li[data-sidebar-id*="/tag/"] > button,
        aside li[data-sidebar-id*="/tag/"] > div > button,
        aside li[data-sidebar-id*="/tag/"] > a,
        aside li[data-sidebar-id*="/tag/"] > div > a {
          padding-left: 0.5rem !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li button .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag-group/"] > ul > li a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"] button .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"] a .scalar-sidebar-indent {
          display: none !important;
        }
        aside li[data-sidebar-id*="/tag/"] [class*="button-label"],
        aside a[href*="#tag/"] [class*="button-label"],
        aside a[href*="#tag/"] div {
          font-size: 0.825rem !important;
          font-weight: 500 !important;
          text-transform: none !important;
          letter-spacing: normal !important;
          color: #e2e8f0 !important;
        }
        aside a[href*="#tag/"]:hover,
        aside li[data-sidebar-id*="/tag/"] button:hover {
          background: rgba(255, 255, 255, 0.06) !important;
          color: #ffffff !important;
        }

        /* 3. ENDPOINTS (TETAP ADA PADDING / MENJOROK KE DALAM DI BAWAH FEATURE) */
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li a,
        aside li[data-sidebar-id*="/tag/"] > ul > li a,
        aside a[href*="#tag/"][href*="/"] {
          padding-left: 1.65rem !important;
        }
        aside li[data-sidebar-id*="/tag-group/"] > ul > li > ul > li a .scalar-sidebar-indent,
        aside li[data-sidebar-id*="/tag/"] > ul > li a .scalar-sidebar-indent {
          display: none !important;
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
    @ResponseBody
    public String scalarDocs() {
        return """
            <!doctype html>
            <html>
              <head>
                <title>Proms ERP - API Reference</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="icon" type="image/svg+xml" href="https://scalar.com/favicon.svg" />
                <style>
            """ + SCALAR_CUSTOM_CSS + """
                </style>
              </head>
              <body>
                <script
                  id="api-reference"
                  data-url="/v3/api-docs"
                  data-configuration='{
                    "theme": "purple",
                    "darkMode": true,
                    "layout": "modern",
                    "showSidebar": true
                  }'>
                </script>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
              </body>
            </html>
            """;
    }
}
