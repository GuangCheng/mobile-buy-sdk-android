/*
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2015 Shopify Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package com.shopify.buy.dataprovider;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * Exception thrown when API call failed, and be returned in {@link Callback#failure(BuyClientError)}
 */
public final class BuyClientError extends RuntimeException {

    /**
     * Represents any errors returning from the backend
     */
    public static final int ERROR_TYPE_API = -2;

    /**
     * Represents any network related errors
     */
    public static final int ERROR_TYPE_NETWORK = -1;

    /**
     * Represent any unknown errors
     */
    public static final int ERROR_TYPE_UNKNOWN = 0;

    private final int type;

    private final Response retrofitResponse;

    private final String retrofitErrorBody;

    private final JSONObject errorsRootJsonObject;

    public BuyClientError(final Throwable throwable) {
        super(throwable);

        retrofitResponse = null;
        retrofitErrorBody = "";

        if (throwable instanceof IOException) {
            type = ERROR_TYPE_NETWORK;
            errorsRootJsonObject = null;
        } else {
            type = ERROR_TYPE_UNKNOWN;
            errorsRootJsonObject = null;
        }
    }

    public BuyClientError(final Response retrofitResponse) {
        this.retrofitResponse = retrofitResponse;

        if (retrofitResponse != null) {
            type = ERROR_TYPE_API;
            retrofitErrorBody = extractRetrofitErrorBody(retrofitResponse);
            errorsRootJsonObject = parseRetrofitErrorResponse(retrofitErrorBody);
        } else {
            type = ERROR_TYPE_UNKNOWN;
            errorsRootJsonObject = null;
            retrofitErrorBody = "";
        }
    }

    /**
     * Returns the type of error, on of: {@link BuyClientError#ERROR_TYPE_API}, {@link BuyClientError#ERROR_TYPE_NETWORK}, {@link BuyClientError#ERROR_TYPE_UNKNOWN}
     *
     * @return error type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns raw retrofit response
     *
     * @return retrofit response
     */
    public Response getRetrofitResponse() {
        return retrofitResponse;
    }

    /**
     * Returns raw retrofit response error body
     *
     * @return retrofit response error body
     */
    public String getRetrofitErrorBody() {
        return retrofitErrorBody;
    }

    /**
     * Returns parsed map of errors ({@code key = "code", value = "message"}).<br>
     * This is the util method that helps to parse json returned in the error body by specified path to the array of errors.<br>
     * For instance, to get all errors related to the customer email, {@code getErrors("customer", "email")} should be called.
     *
     * @param path of nested json fields to array of errors
     * @return map of error codes and messages if found, {@code null} otherwise
     */
    public Map<String, String> getErrors(final String... path) {
        if (errorsRootJsonObject == null) {
            return null;
        }

        if (path == null || path.length == 0) {
            return null;
        }

        final Iterator<String> pathIterator = Arrays.asList(path).iterator();
        JSONObject rootJsonObject = errorsRootJsonObject;
        try {
            while (pathIterator.hasNext()) {
                final String pathElement = pathIterator.next();
                if (rootJsonObject.has(pathElement)) {
                    if (pathIterator.hasNext()) {
                        rootJsonObject = rootJsonObject.getJSONObject(pathElement);
                    } else {
                        return parseErrorMessages(rootJsonObject.getJSONArray(pathElement));
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            //ignore
        }

        return null;
    }

    /**
     * Returns the ordered list of line items errors for specific property. Index of errors corresponds to the index
     * of line item in the checkout. Could contain null items that represents no errors for the particular line item.
     *
     * @param property filter for line item errors (e.g. "quantity")
     * @return ordered list of line items errors
     */
    public List<Map<String, String>> getLineItemErrors(final String property) {
        if (errorsRootJsonObject == null) {
            return null;
        }

        if (TextUtils.isEmpty(property)) {
            return null;
        }

        final Iterator<String> pathIterator = Arrays.asList("checkout", "line_items").iterator();
        JSONObject rootJsonObject = errorsRootJsonObject;
        try {
            JSONArray lineItemJsonErrors = null;
            while (pathIterator.hasNext()) {
                final String pathElement = pathIterator.next();
                if (rootJsonObject.has(pathElement)) {
                    if (pathIterator.hasNext()) {
                        rootJsonObject = rootJsonObject.getJSONObject(pathElement);
                    } else {
                        lineItemJsonErrors = rootJsonObject.getJSONArray(pathElement);
                    }
                } else {
                    return null;
                }
            }

            if (lineItemJsonErrors == null) {
                return null;
            }

            final List<Map<String, String>> lineItemErrors = new ArrayList<>();
            for (int i = 0; i < lineItemJsonErrors.length(); i++) {
                lineItemErrors.add(null);
                if (!lineItemJsonErrors.isNull(i)) {
                    final JSONObject lineItemError = lineItemJsonErrors.getJSONObject(i);
                    if (lineItemError.has(property)) {
                        try {
                            lineItemErrors.set(i, parseErrorMessages(lineItemError.getJSONArray(property)));
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            return lineItemErrors;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRetrofitErrorBody(final Response retrofitResponse) {
        try {
            return retrofitResponse.errorBody().string();
        } catch (Exception e) {
            return "";
        }
    }

    private JSONObject parseRetrofitErrorResponse(final String retrofitErrorBody) {
        if (TextUtils.isEmpty(retrofitErrorBody)) {
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(retrofitErrorBody);
            if (jsonObject.has("errors")) {
                return jsonObject.getJSONObject("errors");
            }
        } catch (JSONException e) {
            //ignore
        }

        return null;
    }

    private Map<String, String> parseErrorMessages(final JSONArray errors) throws JSONException {
        final Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < errors.length(); i++) {
            final JSONObject error = errors.getJSONObject(i);
            result.put(error.getString("code"), error.getString("message"));
        }
        return result;
    }
}
