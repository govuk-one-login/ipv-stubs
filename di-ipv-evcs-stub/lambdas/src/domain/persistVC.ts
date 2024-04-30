import CreateState from "./enums/createState";
import VCMetadata from "./vcMetadata";
import VCProvenience from "./enums/vcProvenience";

export default interface PersistVC {
    vc: string;
    state: CreateState;
    metadata?: VCMetadata;
    provenience?: VCProvenience;
}
