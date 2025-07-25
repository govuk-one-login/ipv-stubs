package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BankAccountDetails;

public class BankAccounts {

    public static final BankAccountDetails BANK_ACCOUNT_VALID =
            createBankAccount("103233", "12345678");

    private BankAccounts() {
        // Replace default public constructor
    }

    private static BankAccountDetails createBankAccount(String sortCode, String accountNumber) {
        return BankAccountDetails.builder()
                .withSortCode(sortCode)
                .withAccountNumber(accountNumber)
                .build();
    }
}
