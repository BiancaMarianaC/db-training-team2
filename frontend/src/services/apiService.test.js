import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
    ApiError,
    createTrade,
    getReconResults,
    getTrades,
    resolveBreak
} from './apiService.js';

function jsonResponse(body, { ok = true, status = 200 } = {}) {
    return {
        ok,
        status,
        json: vi.fn().mockResolvedValue(body)
    };
}

describe('apiService', () => {
    beforeEach(() => {
        global.fetch = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('uses the shared request wrapper for all required endpoints', async () => {
        fetch.mockResolvedValue(jsonResponse({ content: [] }));

        await getTrades({ status: 'PENDING' });
        await createTrade({ tradeRef: 'TRD-2026-0001' });
        await getReconResults({ status: 'OPEN' });
        await resolveBreak(42);

        expect(fetch).toHaveBeenNthCalledWith(1, '/api/v1/trades?status=PENDING', expect.objectContaining({
            headers: expect.objectContaining({ Authorization: expect.stringMatching(/^Basic /) })
        }));
        expect(fetch).toHaveBeenNthCalledWith(2, '/api/v1/trades', expect.objectContaining({
            method: 'POST',
            body: JSON.stringify({ tradeRef: 'TRD-2026-0001' })
        }));
        expect(fetch).toHaveBeenNthCalledWith(3, '/api/v1/recon/results?status=OPEN', expect.any(Object));
        expect(fetch).toHaveBeenNthCalledWith(4, '/api/v1/recon/42/resolve', expect.objectContaining({ method: 'PUT' }));
    });

    it('throws ApiError with the response status and body for non-2xx responses', async () => {
        const body = { message: 'Trade validation failed', details: { price: 'must be positive' } };
        fetch.mockResolvedValue(jsonResponse(body, { ok: false, status: 400 }));

        await expect(createTrade({})).rejects.toMatchObject({
            name: 'ApiError',
            status: 400,
            body
        });
    });

    it('converts network failures into ApiError', async () => {
        fetch.mockRejectedValue(new TypeError('Failed to fetch'));

        await expect(getTrades()).rejects.toEqual(expect.objectContaining({
            name: 'ApiError',
            status: undefined,
            body: {
                message: 'Network request failed',
                cause: 'Failed to fetch'
            }
        }));
    });

    it('returns null for a successful response with no content', async () => {
        fetch.mockResolvedValue(jsonResponse(undefined, { status: 204 }));

        await expect(resolveBreak(42)).resolves.toBeNull();
    });

    it('creates instances of the exported typed error', () => {
        expect(new ApiError(500, { message: 'Server error' })).toBeInstanceOf(ApiError);
    });
});
