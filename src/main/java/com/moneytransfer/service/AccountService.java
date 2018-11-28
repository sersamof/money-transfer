package com.moneytransfer.service;

import com.moneytransfer.dao.DAOFactory;
import com.moneytransfer.exception.CustomException;
import com.moneytransfer.model.Account;
import com.moneytransfer.model.MoneyUtil;
import com.moneytransfer.utils.Utils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Account Service
 */
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountService {

  private final DAOFactory daoFactory = DAOFactory.getDAOFactory(Utils.getStringProperty("daoImplementation"));

  private static Logger log = Logger.getLogger(AccountService.class);

  /**
   * Find all accounts
   *
   * @return
   * @throws CustomException
   */
  @GET
  @Path("/all")
  public Response getAllAccounts() throws CustomException {
    Set<Account> account = daoFactory.getAccountDAO().getAllAccounts();
    return Response.ok("[" + account.stream().map(Account::toString).collect(Collectors.joining(",")) + "]").build();
  }

  /**
   * Find by account id
   *
   * @param accountId
   * @return
   * @throws CustomException
   */
  @GET
  @Path("/{accountId}")
  public Response getAccount(@PathParam("accountId") long accountId) throws CustomException {
    return Response.ok(daoFactory.getAccountDAO().getAccountById(accountId).toString()).build();
  }

  @GET
  @Path("/by_user/{user}/{code}")
  public Response getAccount(@PathParam("user") String user, @PathParam("code") String code) throws CustomException {
    return Response.ok(daoFactory.getAccountDAO().getAccountByUser(user, code).toString()).build();
  }

  /**
   * Find balance by account Id
   *
   * @param accountId
   * @return
   * @throws CustomException
   */
  @GET
  @Path("/{accountId}/balance")
  public BigDecimal getBalance(@PathParam("accountId") long accountId) throws CustomException {
    final Account account = daoFactory.getAccountDAO().getAccountById(accountId);

    if (account == null) {
      throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
    }
    return account.getBalance();
  }

  /**
   * Create Account
   *
   * @param account
   * @return
   * @throws CustomException
   */
  @POST
  @Path("/create")
  public Response createAccount(Account account) throws CustomException {
    final long accountId = daoFactory.getAccountDAO().createAccount(account);
    return Response.ok(daoFactory.getAccountDAO().getAccountById(accountId).toString()).build();
  }

  /**
   * Deposit amount by account Id
   *
   * @param accountId
   * @param amount
   * @return
   * @throws CustomException
   */
  @PUT
  @Path("/{accountId}/deposit/{amount}")
  public Response deposit(@PathParam("accountId") long accountId, @PathParam("amount") BigDecimal amount) throws CustomException {

    if (amount.compareTo(MoneyUtil.zeroAmount) <= 0) {
      throw new WebApplicationException("Invalid Deposit amount", Response.Status.BAD_REQUEST);
    }

    daoFactory.getAccountDAO().updateAccountBalance(accountId, amount.setScale(4, RoundingMode.HALF_EVEN));
    return Response.ok(daoFactory.getAccountDAO().getAccountById(accountId).toString()).build();
  }

  /**
   * Withdraw amount by account Id
   *
   * @param accountId
   * @param amount
   * @return
   * @throws CustomException
   */
  @PUT
  @Path("/{accountId}/withdraw/{amount}")
  public Response withdraw(@PathParam("accountId") long accountId, @PathParam("amount") BigDecimal amount) throws CustomException {
    if (amount.compareTo(MoneyUtil.zeroAmount) <= 0) {
      throw new WebApplicationException("Invalid Deposit amount", Response.Status.BAD_REQUEST);
    }
    BigDecimal delta = amount.negate();
    if (log.isDebugEnabled()) {
      log.debug("Withdraw service: delta change to account  " + delta + " Account ID = " + accountId);
    }
    daoFactory.getAccountDAO().updateAccountBalance(accountId, delta.setScale(4, RoundingMode.HALF_EVEN));
    return Response.ok(daoFactory.getAccountDAO().getAccountById(accountId).toString()).build();
  }

  /**
   * Delete amount by account Id
   *
   * @param accountId
   * @return
   * @throws CustomException
   */
  @DELETE
  @Path("/{accountId}")
  public Response deleteAccount(@PathParam("accountId") long accountId) throws CustomException {
    int deleteCount = daoFactory.getAccountDAO().deleteAccountById(accountId);
    if (deleteCount == 1) {
      return Response.status(Response.Status.OK).build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
