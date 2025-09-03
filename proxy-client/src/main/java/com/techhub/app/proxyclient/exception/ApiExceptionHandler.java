package com.techhub.app.proxyclient.exception;

import com.techhub.app.proxyclient.exception.payload.ExceptionMsg;
import com.techhub.app.proxyclient.exception.payload.GlobalReponseException;
import com.techhub.app.proxyclient.exception.wrapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.FeignException.FeignClientException;
import feign.FeignException.FeignServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.common.dbiz.dto.paymentDto.napas.NapasResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

	private final ObjectMapper objectMapper;

	@ExceptionHandler(value = {
		FeignClientException.class,
		FeignServerException.class,
		FeignException.class
	})
//	public <T extends FeignException> ResponseEntity<ExceptionMsg> handleProxyException(final T e) {
		public <T extends FeignException> ResponseEntity<?> handleProxyException(final T e) throws IOException {
		e.printStackTrace();
		if(e.request() != null &&
				(e.request().url().contains("napas/notification") ||
						e.request().url().contains("napas/investigation") ||
						e.request().url().contains("napas/reconciliation"))){
			log.info("**ApiExceptionHandler controller, handle feign proxy exception for napas*\n");
			return new ResponseEntity<>(
					e.contentUTF8(), HttpStatus.valueOf(e.status()));
		}
		log.info("**ApiExceptionHandler controller, handle feign proxy exception*\n"); // check
		final var badRequest = HttpStatus.INTERNAL_SERVER_ERROR;
		log.info("Log 1: " + e.getMessage());
		String errorMessageFromServiceA = e.contentUTF8();
		log.info("Log error: " + errorMessageFromServiceA);
//		String extractedMessage = "Unknown error occurred";
//		try {
//			ObjectMapper objectMapper = new ObjectMapper();
//			JsonNode jsonNode = objectMapper.readTree(errorMessageFromServiceA);
//			log.info("Log error: " + jsonNode.toPrettyString());
//			log.info("Log error1: " + jsonNode.toPrettyString());
//
//			extractedMessage = jsonNode.path("msg").asText();
//
//		} catch (IOException ex) {
//			extractedMessage=e.contentUTF8();
//			log.error("Failed to parse JSON from Service A: {}", ex.getMessage());
//		}


		return new ResponseEntity<>(
				GlobalReponseException.builder()
						.errors(errorMessageFromServiceA)
						.message(errorMessageFromServiceA)
						.data(null)
						.status(badRequest.value())
						.build(), badRequest);
	}

	@ExceptionHandler(value = {
		MethodArgumentNotValidException.class,
		HttpMessageNotReadableException.class,
			BindException.class
	})
	public <T extends BindException> ResponseEntity<ExceptionMsg> handleValidationException(final T e) {

		log.info("**ApiExceptionHandler controller, handle validation exception*\n");
		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.error(e.getMessage())
					.status(badRequest)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), badRequest);
	}

	@ExceptionHandler(value = {
		UserObjectNotFoundException.class,
		CredentialNotFoundException.class,
		VerificationTokenNotFoundException.class,
		FavouriteNotFoundException.class,
		IllegalStateException.class
	})
	public <T extends RuntimeException> ResponseEntity<ExceptionMsg> handleApiRequestException(final T e)
	{

		log.info("**ApiExceptionHandler controller, handle API request*\n");
		final var badRequest = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(
				ExceptionMsg.builder()
					.error(e.getMessage())
					.status(badRequest)
					.timestamp(ZonedDateTime
							.now(ZoneId.systemDefault()))
					.build(), badRequest);
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ExceptionMsg> handleAllExceptions(Exception e) {
		log.error("**ApiExceptionHandler controller, handle unexpected exception*\n", e);
		final var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
		return new ResponseEntity<>(
				ExceptionMsg.builder()
						.error("An unexpected error occurred: " + e.getMessage())
						.status(internalServerError)
						.timestamp(ZonedDateTime.now(ZoneId.systemDefault()))
						.build(), internalServerError);
	}


	@ExceptionHandler(InvalidCredentialException.class)
	public ResponseEntity<NapasResponseDto> handleInvalidCredentialException(InvalidCredentialException e) {
		log.error("**ApiExceptionHandler controller, handle invalid credential exception*\n", e);
		final var badRequest = HttpStatus.BAD_REQUEST;
		return new ResponseEntity<>(
				NapasResponseDto.builder()
						.message("failure")
						.description("Invalid username/password")
						.build(), badRequest);
	}

	@ExceptionHandler(ExpiredTokenException.class)
	public ResponseEntity<NapasResponseDto> handleInvalidCredentialException(ExpiredTokenException e) {
		log.error("**ApiExceptionHandler controller, handle expired token exception*\n", e);
		final var unauthorized = HttpStatus.UNAUTHORIZED;
		return new ResponseEntity<>(
				NapasResponseDto.builder()
						.code("failure")
						.message("Token has expired")
						.build(), unauthorized);
	}

//	@ExceptionHandler(NapasErrorException.class)
//	public ResponseEntity<Object> handleNapasErrorException(NapasErrorException napasErrorException) {
//		log.error("NapasErrorException controller, handle NapasErrorException\n");
//		String error = napasErrorException.getMessage();
//		log.info("Log 2: " + napasErrorException.getMessage());
//		final var badRequest = HttpStatus.BAD_REQUEST;
//		return new ResponseEntity<>(
//				NapasResponseDto.builder()
//						.code("failure")
//						.message(error)
//						.build(), badRequest);
//	}


	private NapasResponseDto toNapasResponse(String error) {

		return NapasResponseDto.builder()
				.code("failure")
				.message(error)
				.build();
	}
}










