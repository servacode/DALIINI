import { OperationPage } from "../../../components/operations/operation-page";
import { operationRoutes } from "../../../lib/operations/catalog";

export default function Page() {
  return <OperationPage route={operationRoutes.reviews} />;
}
