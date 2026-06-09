package com.kitehub.branding.service.banner;

import com.kitehub.branding.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlaywrightBannerRenderer} — the sidecar render path
 * (GAP-1135). HTTP is exercised via a stub {@link ExchangeFunction}; storage via
 * a Mockito mock. The real Chromium sidecar is NOT involved.
 */
class PlaywrightBannerRendererTest {

    private static final byte[] WEBP = new byte[] {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' };
    private static final BannerComposition COMPOSITION =
            new BannerComposition("<html><body>banner</body></html>", 1200, 630);
    private static final UUID INSTANCE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static WebClient webClientReturning(byte[] body) {
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(body);
        ExchangeFunction ef = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "image/webp")
                .body(Flux.just(buffer))
                .build());
        return WebClient.builder().exchangeFunction(ef).build();
    }

    private static WebClient webClientFailing() {
        ExchangeFunction ef = request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("boom")
                .build());
        return WebClient.builder().exchangeFunction(ef).build();
    }

    @Test
    void render_uploadsWebpAndReturnsPresignedUrl_onSuccess() {
        S3StorageService storage = mock(S3StorageService.class);
        when(storage.getPresignedAssetUrl(anyString())).thenReturn("https://cdn.example/banner.webp");
        PlaywrightBannerRenderer renderer = new PlaywrightBannerRenderer(
                "http://sidecar:3000/render", 30, storage, webClientReturning(WEBP));

        String url = renderer.render(COMPOSITION, INSTANCE);

        assertThat(renderer.isAvailable()).isTrue();
        assertThat(url).isEqualTo("https://cdn.example/banner.webp");
        verify(storage).uploadAsset(
                any(InputStream.class),
                startsWith("banners/" + INSTANCE + "/"),
                eq("image/webp"),
                eq((long) WEBP.length));
    }

    @Test
    void render_returnsNull_andSkipsUpload_whenUrlBlank() {
        S3StorageService storage = mock(S3StorageService.class);
        PlaywrightBannerRenderer renderer = new PlaywrightBannerRenderer(
                "", 30, storage, (WebClient) null);

        String url = renderer.render(COMPOSITION, INSTANCE);

        assertThat(url).isNull();
        assertThat(renderer.isAvailable()).isFalse();
        verify(storage, never()).uploadAsset(any(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void render_returnsNull_onSidecarError() {
        S3StorageService storage = mock(S3StorageService.class);
        PlaywrightBannerRenderer renderer = new PlaywrightBannerRenderer(
                "http://sidecar:3000/render", 30, storage, webClientFailing());

        String url = renderer.render(COMPOSITION, INSTANCE);

        assertThat(url).isNull();
        verify(storage, never()).uploadAsset(any(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void render_returnsNull_onEmptySidecarBody() {
        S3StorageService storage = mock(S3StorageService.class);
        PlaywrightBannerRenderer renderer = new PlaywrightBannerRenderer(
                "http://sidecar:3000/render", 30, storage, webClientReturning(new byte[0]));

        String url = renderer.render(COMPOSITION, INSTANCE);

        assertThat(url).isNull();
        verify(storage, never()).uploadAsset(any(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}
