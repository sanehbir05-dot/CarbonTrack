import { Link } from 'react-router-dom'
import EmptyState from '../components/EmptyState'
import PageHead from '../components/PageHead'
import { usePageTitle } from '../utils/usePageTitle'

export default function NotFound() {
  usePageTitle('Page not found')
  return (
    <>
      <PageHead title="Page not found" />
      <div className="panel">
        <EmptyState
          title="Nothing here"
          message="The page you are looking for does not exist."
          action={
            <Link to="/" className="pill-btn dark">
              Back to dashboard
            </Link>
          }
        />
      </div>
    </>
  )
}
