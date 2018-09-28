alter table rpt.contract_meta drop constraint contract_meta_pkey;
alter table rpt.contract_meta drop column report_type;
alter table rpt.contract_meta add constraint contract_meta_pkey primary key (party_id, contract_id);