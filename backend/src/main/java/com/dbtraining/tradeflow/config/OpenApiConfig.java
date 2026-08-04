package com.dbtraining.tradeflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * OpenApiConfig — TICKET-I063
 * ============================================================================
 * WHAT:    Customises the OpenAPI document Springdoc generates.
 * HOW:     Single @Bean that returns an OpenAPI with project metadata.
 * WHY:     Swagger UI on /swagger-ui.html becomes the single source of truth
 *          for the API contract — the front-end team uses it instead of
 *          digging through controllers.
 * OBSERVE: After this is wired, the title in the top-left of Swagger UI is
 *          "TradeFlow API".
 * ============================================================================
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI tradeflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeFlow API")
                        .description("Trade reconciliation REST API — Deutsche Bank TDI 2026 case study.")
                        .version("v1")
                        .contact(new Contact()
                                .name("TradeFlow Team")
                                .email("tradeflow@dbtraining.example"))
                        .license(new License()
                                .name("Internal — Deutsche Bank TDI")))
                // Adds the "Authorize" lock button in Swagger UI: set
                // username/password once (e.g. trader/trader-pw), it's then
                // sent as an HTTP Basic header on every "Try it out" call
                // until the page is closed — matches SecurityConfig's
                // .httpBasic() scheme, which springdoc doesn't auto-detect.
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
    }
}