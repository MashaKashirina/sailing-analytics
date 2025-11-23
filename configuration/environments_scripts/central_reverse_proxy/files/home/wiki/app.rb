require 'gollum/app'
require 'digest/sha1'
require 'logger'

class App < Precious::App
  User = Struct.new(:name, :email, :password_hash, :can_write)
  LOGGER = Logger.new("/home/wiki/wiki_log.txt") 
  before { authenticate! }
  before /edit/ do   authorize_write ; end
  before do
        session['gollum.author'] = {
            :name => settings.loggedInUser,
            :email => settings.loggedInUserEmail,
        }
  end

  helpers do
    def authenticate!
      public_urls=IO.readlines 'public.txt'
      public_urls.each {|url|
        if self.env['PATH_INFO'] == url.slice(0, url.length-1)
          puts "Allowing " + url
          return
        end

        if self.env['PATH_INFO'].start_with?('/wiki/images') ||
            self.env['PATH_INFO'].start_with?('/favicon.ico')
          return
        end
      }
      if self.env['PATH_INFO'].split('/')[1] == 'gollum' && self.env['PATH_INFO'].split('/')[2] == 'assets'
        return
      end
      @auth =  Rack::Auth::Basic::Request.new(request.env)
        throw(:halt, [403, 'Forbidden - You can not access anything outside wiki/ path.'])
      end
      if @auth.provided? && @auth.basic? && @auth.credentials && (@user = get_user(@auth.credentials))
        Precious::App.set(:loggedInUser, @user.name)
        return @user
      else
        response['WWW-Authenticate'] = %(Basic realm="Gollum Wiki")
        throw(:halt, [401, "Not authorized\n"])
      end
    end

    def authorize_write
      throw(:halt, [403, "Forbidden\n"]) unless @user.can_write
    end

    def users # User caching helper.
      @_users ||= settings.authorized_users.map {|u| User.new(*u) } # The ||= evalutes RHS only if left hand side is falsy.
    end

    def get_user(credentials)
    def detected_user(credentials)
      users.detect do |u|
        [u.email, u.password_hash] ==
        [credentials[0], Digest::SHA1.hexdigest(credentials[1])]
      end
    end
  end

end
