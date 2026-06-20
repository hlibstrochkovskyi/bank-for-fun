package com.ledgerbank.web;

import com.ledgerbank.ledger.CurrencyMismatchException;
import com.ledgerbank.ledger.InsufficientFundsException;
import com.ledgerbank.ledger.PostingNotFoundException;
import com.ledgerbank.ledger.ReversalNotAllowedException;
import com.ledgerbank.ledger.UnbalancedPostingException;
import com.ledgerbank.payments.HeldTransferNotFoundException;
import com.ledgerbank.payments.IdempotencyConflictException;
import com.ledgerbank.ratelimit.RateLimitExceededException;
import com.ledgerbank.shared.AccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to RFC 7807 {@code application/problem+json} responses.
 * Framework-level exceptions (validation, missing headers, unreadable bodies) are
 * already rendered as ProblemDetail by Spring (spring.mvc.problemdetails.enabled).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(AccountNotFoundException.class)
	ProblemDetail handleNotFound(AccountNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Account not found", ex.getMessage());
	}

	@ExceptionHandler(PostingNotFoundException.class)
	ProblemDetail handlePostingNotFound(PostingNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Posting not found", ex.getMessage());
	}

	@ExceptionHandler(ReversalNotAllowedException.class)
	ProblemDetail handleReversalNotAllowed(ReversalNotAllowedException ex) {
		return problem(HttpStatus.CONFLICT, "Reversal not allowed", ex.getMessage());
	}

	@ExceptionHandler(InsufficientFundsException.class)
	ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds", ex.getMessage());
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Idempotency conflict", ex.getMessage());
	}

	@ExceptionHandler(HeldTransferNotFoundException.class)
	ProblemDetail handleHeldTransferNotFound(HeldTransferNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Held transfer not found", ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail handleIllegalState(IllegalStateException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	@ExceptionHandler({UnbalancedPostingException.class, CurrencyMismatchException.class})
	ProblemDetail handleUnprocessable(RuntimeException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable money operation", ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Forbidden", "You do not have access to this resource.");
	}

	@ExceptionHandler(RateLimitExceededException.class)
	ProblemDetail handleRateLimited(RateLimitExceededException ex) {
		return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many requests",
				"Rate limit exceeded; please slow down and retry shortly.");
	}

	@ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
	ProblemDetail handleBadRequest(RuntimeException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
