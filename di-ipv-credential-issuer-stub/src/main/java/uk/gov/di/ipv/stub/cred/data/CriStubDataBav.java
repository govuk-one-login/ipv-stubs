package uk.gov.di.ipv.stub.cred.data;

import uk.gov.di.model.BankAccountDetails;
import uk.gov.di.model.BirthDate;
import uk.gov.di.model.IdentityCheckSubject;
import uk.gov.di.model.Name;

import java.util.List;

public class CriStubDataBav {

    private CriStubDataBav() {
        // Replace default public constructor
    }

    public static final List<IdentityCheckSubjectCriStubData> Data = List.of(
            createData("Alice Parker BAV", Names.Alice_Jane_Parker, BirthDates.ALICE_PARKER, BankAccounts.BANK_ACCOUNT_VALID),
            createData("Bob Parker BAV", Names.Bob_Parker, BirthDates.BOB_PARKER, BankAccounts.BANK_ACCOUNT_VALID),
            createData("James Moriarty BAV", Names.James_Moriarty, BirthDates.JAMES_MORIARTY, BankAccounts.BANK_ACCOUNT_VALID),
            createData("Kenneth Decerqueira BAV", Names.Kenneth_Decerqueira, BirthDates.KENNETH_DECERQUEIRA, BankAccounts.BANK_ACCOUNT_VALID),
            createData("Mary Watson BAV", Names.Mary_Watson, BirthDates.MARY_WATSON, BankAccounts.BANK_ACCOUNT_VALID)
    );

    private static IdentityCheckSubjectCriStubData createData(String label, Name name, BirthDate birthDate, BankAccountDetails bankAccountDetails) {
        return IdentityCheckSubjectCriStubData.builder()
                .label(label)
                .payload(IdentityCheckSubject.builder()
                        .withName(List.of(name))
                        .withBirthDate(List.of(birthDate))
                        .withBankAccount(List.of(bankAccountDetails))
                        .build()).build();
    }
}

