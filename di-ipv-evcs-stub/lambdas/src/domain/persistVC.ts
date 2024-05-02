import CreateState from "./enums/createState";
import VCMetadata from "./vcMetadata";
import VCProvenance from "./enums/vcProvenance";

export default interface PersistVC {
    vc: string;
    state: CreateState;
    metadata?: VCMetadata;
    provenance?: VCProvenance;
}
